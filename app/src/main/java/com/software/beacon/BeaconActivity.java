package com.software.beacon;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class BeaconActivity extends AppCompatActivity implements BeaconConsumer, ValidationResponse {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final String BEACON_TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    private TextView tv;
    double minDistance = Double.MAX_VALUE;
    BeaconDurationCalculator beaconDurationCalculator;
    private RecyclerView popRecyclerView;
    private RecyclerView.Adapter popAdapter;
    private List<DealsOffersItem> listItems;
    private boolean isActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.setForegroundBetweenScanPeriod(8000l);
        beaconManager.bind(this);
        beaconDurationCalculator = new BeaconDurationCalculator();


        tv = (TextView) findViewById(R.id.textView);

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
        beaconManager.unbind(this);
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
                        }
                        else {
                            if(!isActive)
                                BeaconActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        onButtonShowPopupWindowClick(beaconDurationCalculator.getBeaconId().toString());
                                    }
                                });
                            isActive = true;

/*                            tv.post(new Runnable() {
                                @Override
                                public void run() {
                                    tv.append("Distance: " + beaconDurationCalculator.getBeaconDistance() + " Id: " + beaconDurationCalculator.getBeaconId() + "\n");
                                }
                            });*/
                        }

                    }
/*                    tv.post(new Runnable() {
                        @Override
                        public void run() {
                            tv.append("Distance: " + oneBeacon.getDistance() + " Id: " + oneBeacon.getId1() + "/" + oneBeacon.getId2() + "/" + oneBeacon.getId3() + "\n");
                        }
                    });*/
                    Log.d(BEACON_TAG, "distance: " + oneBeacon.getDistance() + " Id: " + oneBeacon.getId1() + "/" + oneBeacon.getId2() + "/" + oneBeacon.getId3());
                    //Log.d(BEACON_TAG, minDistance + " " + oldBeacon + " " + newBeacon);
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onButtonShowPopupWindowClick(String beacon) {

        // get a reference to the already created main layout
        LinearLayout mainLayout = (LinearLayout)
                findViewById(R.id.activity_main_layout);

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);


        Get_Result conn = new Get_Result(this);
        conn.delegate = BeaconActivity.this;
        String type = "S";
        if(beacon.equals("1")) type = "H";
        String query = "select * from product left JOIN deals on product.product_id = deals.deals_id where product_type='"+type+"'";
        conn.execute(URLS.Fetch_Product_URL, query);

        popRecyclerView = (RecyclerView) popupView.findViewById(R.id.recycler_view_popup);
        popRecyclerView.setHasFixedSize(true);
        popRecyclerView.setItemViewCacheSize(20);
        popRecyclerView.setDrawingCacheEnabled(true);
        popRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        popRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        listItems = new ArrayList<>();
        popAdapter = new DealsOffersAdapter(listItems, this);
        popRecyclerView.setAdapter(popAdapter);

        // show the popup window
        popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    @Override
    public void response(boolean result, String s) {
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
                    String priceDetails[] = {"","","",""};
                    if (discount != null) {
                        discount = detail.getDouble("discount");
                        discounted_price = Math.round(((100 - discount) / 100) * price);
                        priceDetails[0] = rs + price + "";
                        priceDetails[1] = discount + "";
                        priceDetails[2] = rs + "(" + discounted_price + "% OFF)";
                        priceDetails[3] = validTill + getDate(detail.getString("end_date"));
                        DealsOffersItem listItem = new DealsOffersItem(detail.getString("product_name"), priceDetails[0], detail.getString("product_image_path"), priceDetails[2], priceDetails[1], priceDetails[3]);
                        listItems.add(listItem);
                    }
                }
                popAdapter.notifyDataSetChanged();

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private String getDate(String timeStamp){

        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timeStamp);
            SimpleDateFormat new_sdf = new SimpleDateFormat("dd-MMM-yyyy");
            return new_sdf.format(date);
        }
        catch(Exception ex){
            return "xx";
        }
    }
}
