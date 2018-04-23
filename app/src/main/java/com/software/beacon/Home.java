package com.software.beacon;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.software.beacon.URLS.mCartItemCount;


public class Home extends AppCompatActivity implements BeaconConsumer, NavigationView.OnNavigationItemSelectedListener, ValidationResponse, HomeResponse {

    private SessionManager session;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle mToggle;
    TextView textCartItemCount;
    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        unsetBeacon();
                        Snackbar snackbar = Snackbar
                                .make(findViewById(R.id.activity_main_layout), "Bluetooth Off", Snackbar.LENGTH_INDEFINITE)
                                .setAction("TURN ON", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mBluetoothAdapter.enable();
                                    }
                                });

                        snackbar.show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        setBeacon();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    /**/
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final String BEACON_TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    double minDistance = Double.MAX_VALUE;
    BeaconDurationCalculator beaconDurationCalculator;
    private RecyclerView popRecyclerView;
    private RecyclerView.Adapter popAdapter;
    private List<DealsOffersItem> listItems;
    private boolean isActive = false;
    BackgroundPowerSaver backgroundPowerSaver;
    /**/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(getApplicationContext());
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        TextView userName = (TextView) headerView.findViewById(R.id.user_name);
        userName.setText("Hi, " + session.getUserDetails().get(session.KEY_NAME) + "!");

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        if (mBluetoothAdapter == null) {
            Snackbar.make(findViewById(R.id.activity_main_layout), "Bluetooth not Supported!", Snackbar.LENGTH_INDEFINITE).show();
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                beaconManager = BeaconManager.getInstanceForApplication(this);
                beaconManager.getBeaconParsers().add(new BeaconParser()
                        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
                backgroundPowerSaver = new BackgroundPowerSaver(this);
                beaconManager.setBackgroundBetweenScanPeriod(2000l);
                beaconManager.setForegroundBetweenScanPeriod(2000l);
                beaconManager.bind(this);
                beaconDurationCalculator = new BeaconDurationCalculator();
            } else {
                Snackbar snackbar = Snackbar
                        .make(findViewById(R.id.activity_main_layout), "Bluetooth Off", Snackbar.LENGTH_INDEFINITE)
                        .setAction("TURN ON", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mBluetoothAdapter.enable();
                            }
                        });

                snackbar.show();
            }
        }

        /**/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
        /**/

    }

    public void setBeacon() {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
            backgroundPowerSaver = new BackgroundPowerSaver(this);
            beaconManager.setBackgroundBetweenScanPeriod(2000l);
            beaconManager.setForegroundBetweenScanPeriod(2000l);
            beaconManager.bind(this);
            beaconDurationCalculator = new BeaconDurationCalculator();
        }
    }

    public void unsetBeacon() {
        if(beaconManager!=null)
            beaconManager.unbind(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("permission", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (beaconManager != null)
            beaconManager.unbind(this);
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBeaconServiceConnect() {
        final Region region = new Region("myBeacons", null, null, null);
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                try {
                    Log.d(BEACON_TAG, "didEnterRegion");
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                try {
                    Log.d(BEACON_TAG, "didExitRegion");
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        /* Beacon Scanning and Distance Finder */
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                minDistance = Double.MAX_VALUE;
                Log.d(BEACON_TAG, "didRangeBeaconsInRegion");
                for (final Beacon oneBeacon : beacons) {
                    if (oneBeacon.getDistance() < minDistance) {
                        minDistance = oneBeacon.getDistance();
                        if (!beaconDurationCalculator.getBeaconId().toString().equals(oneBeacon.getId3().toString())) {
                            isActive = false;
                            beaconDurationCalculator.activate(oneBeacon.getId3().toString(), oneBeacon.getDistance());
                        } else {
                            if (!isActive)
                                Home.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        populateOffers(beaconDurationCalculator.getBeaconId().toString());
                                    }
                                });
                            isActive = true;
                        }
                    }
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void populateOffers(String beacon) {
        Get_Result conn = new Get_Result(this);
        conn.homeDelegate = Home.this;
        String type = "S";
        if (beacon.equals("1")) type = "H";
        String query = "select * from product left JOIN deals on product.product_id = deals.deals_id where product_type='" + type + "'";
        conn.execute(URLS.Fetch_Product_URL, query, "home");

        popRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_popup);
        popRecyclerView.setHasFixedSize(true);
        popRecyclerView.setItemViewCacheSize(20);
        popRecyclerView.setDrawingCacheEnabled(true);
        popRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        popRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        listItems = new ArrayList<>();
        popAdapter = new DealsOffersAdapter(listItems, this);
        popRecyclerView.setAdapter(popAdapter);
    }

    @Override
    public void homeResponse(boolean result, String s) {
        String rs = this.getResources().getString(R.string.Rs);
        String validTill = this.getResources().getString(R.string.validity);
        if (result) {
            try {
                JSONArray data = new JSONArray(s);

                for (int i = 0; i < data.length(); i++) {
                    JSONObject detail = data.getJSONObject(i);
                    double price = detail.getDouble("product_price");
                    double discounted_price = 0.0;
                    Double discount = detail.isNull("discount") ? null : detail.getDouble("discount");
                    String priceDetails[] = {"", "", "", ""};
                    if (discount != null) {
                        discount = detail.getDouble("discount");
                        discounted_price = Math.round(((100 - discount) / 100) * price);
                        priceDetails[0] = rs + price + "";
                        priceDetails[1] = discount + "% \nOFF";
                        priceDetails[2] = rs + discounted_price;
                        priceDetails[3] = validTill + getDate(detail.getString("end_date"));
                        DealsOffersItem listItem = new DealsOffersItem(detail.getString("product_name"), priceDetails[0], detail.getString("product_image_path"), priceDetails[2], priceDetails[1], priceDetails[3]);
                        listItems.add(listItem);
                    }
                }
                sendNotification("New Offer Only For You!!!");
                popAdapter.notifyDataSetChanged();

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private String getDate(String timeStamp) {

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timeStamp);
            SimpleDateFormat new_sdf = new SimpleDateFormat("dd-MMM-yyyy");
            return new_sdf.format(date);
        } catch (Exception ex) {
            return "xx";
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.scan_menu, menu);

        final MenuItem menuItem = menu.findItem(R.id.menu_shoppping_cart);

        View actionView = MenuItemCompat.getActionView(menuItem);
        textCartItemCount = (TextView) actionView.findViewById(R.id.cart_badge);
        setupBadge();
        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(menuItem);
            }
        });
        return true;
    }

    private void setupBadge() {
        if (textCartItemCount != null) {
            if (mCartItemCount == 0) {
                if (textCartItemCount.getVisibility() != View.GONE) {
                    textCartItemCount.setVisibility(View.GONE);
                }
            } else {
                textCartItemCount.setText(String.valueOf(Math.min(mCartItemCount, 99)));
                if (textCartItemCount.getVisibility() != View.VISIBLE) {
                    textCartItemCount.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                session.logoutUser();
                finish();
                break;
            case R.id.menu_scan_beacons:
                Intent intent = new Intent(Home.this, BeaconActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_shoppping_cart:
                intent = new Intent(Home.this, CartActivity.class);
                startActivity(intent);
                break;

        }
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_account) {
            Intent i = new Intent(Home.this, MyAccount.class);
            startActivity(i);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        } else if (id == R.id.nav_logout) {
            session.logoutUser();
            finish();
        } else if (id == R.id.nav_scan) {
            Intent i = new Intent(Home.this, ScanAndBuy.class);
            startActivity(i);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        } else if (id == R.id.nav_offer) {
            Intent i = new Intent(Home.this, DealsOffers.class);
            startActivity(i);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Get_Result conn = new Get_Result(this);
        conn.delegate = Home.this;
        String query = "select * from user_cart where user_id = '" + session.getUserDetails().get(session.KEY_MOB) + "'";
        conn.execute(URLS.Fetch_Product_URL, query);
        setupBadge();
    }

    @Override
    public void response(boolean result, String s) {
        int total = 0;
        if (result) {
            try {
                JSONArray data = new JSONArray(s);
                for (int i = 0; i < data.length(); i++) {
                    JSONObject detail = data.getJSONObject(i);
                    total += detail.getInt("quantity");
                }
                mCartItemCount = total;
                setupBadge();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            mCartItemCount = total;
            setupBadge();
        }
    }

    void sendNotification(String newMsg) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(Home.this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("Shop Easy");
        mBuilder.setContentText(newMsg);
        mBuilder.setAutoCancel(true);
        mBuilder.setDefaults(-1);
        Intent notificationIntent = new Intent(this, Home.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(001, mBuilder.build());
    }
}
