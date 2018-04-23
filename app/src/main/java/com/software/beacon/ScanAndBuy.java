package com.software.beacon;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.Manifest.permission.CAMERA;
import static com.software.beacon.URLS.mCartItemCount;

public class ScanAndBuy extends AppCompatActivity implements ZXingScannerView.ResultHandler, ValidationResponse {

    private static final int REQUEST_CAMERA = 1;
    private ZXingScannerView scannerView;
    private PopupWindow mPopup;
    private Toolbar mToolbar;
    TextView textCartItemCount;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_and_buy);
        //scannerView = new ZXingScannerView(this);
        scannerView = (ZXingScannerView) findViewById(R.id.zxscan);

        mToolbar = (Toolbar) findViewById(R.id.toolbar_scan_and_buy);
        session = new SessionManager(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                requestPermission();
            }
        }
    }

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(ScanAndBuy.this, CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
    }

    public void onRequestPermissionResult(int requestCode, String permission[], int grantResults[]) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults.length > 0) {
                    boolean cammeraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (!cammeraAccepted) {
                        Toast.makeText(ScanAndBuy.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                displayAlertMessage("You need to allow access to both permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()) {
                if (scannerView == null) {
                    scannerView = new ZXingScannerView(this);
                    setContentView(scannerView);
                }
                scannerView.setResultHandler(this);
                scannerView.startCamera();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scannerView.stopCamera();
    }

    public void displayAlertMessage(String message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(ScanAndBuy.this)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", null)
                .create();
    }

    @Override
    public void handleResult(Result result) {
        final String scanResult = result.getText();
        Log.d("dopost", scanResult);
        Get_Result conn = new Get_Result(this);
        conn.delegate = ScanAndBuy.this;
        String query = "select * from product where product_id = '" + scanResult + "'";
        showProgress(true);
        conn.execute(URLS.Fetch_Product_URL, query);
    }

    @Override
    public void response(boolean result, String s) {
        showProgress(false);
        String scanResult = " ";
        JSONObject detail = null;
        String product_id = "";
        final boolean[] showDialog = {false};
        if (result) {
            try {
                JSONArray data = new JSONArray(s);
                detail = data.getJSONObject(0);
                scanResult = "Name : " + detail.getString("product_name") + "\nPrice : " + detail.getDouble("product_price");
                product_id = detail.getString("product_id");
                mCartItemCount++;
                setupBadge();
                showDialog[0] = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            scanResult = (s.equals("None")) ? "No Result !" : s;
        }

        if (showDialog[0]) {
            Log.e("dopost", showDialog[0] + "");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Scan Result");
            final String finalProduct_id = product_id;
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    Get_Result conn = new Get_Result(ScanAndBuy.this);
                    conn.delegate = ScanAndBuy.this;
                    String query = "insert into user_cart values('" + session.getUserDetails().get(session.KEY_MOB) +
                            "','" + finalProduct_id + "', 1) ON DUPLICATE KEY UPDATE quantity = quantity + 1";
                    Log.e("dopost", query);
                    showDialog[0] = false;
                    conn.execute(URLS.User_Cart, query);
                    scannerView.resumeCameraPreview(ScanAndBuy.this);
                }
            });
            builder.setCancelable(false);
            builder.setMessage(scanResult);
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            if (!s.equals("ok")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Scan Result");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        scannerView.resumeCameraPreview(ScanAndBuy.this);
                    }
                });
                builder.setCancelable(false);
                builder.setMessage(scanResult);
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    public void showProgress(final boolean show) {
        View popupView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.my_progress, null);
        if (show) {
            Point size = new Point();
            this.getWindowManager().getDefaultDisplay().getSize(size);
            int width = size.x;
            int height = size.y;
            mPopup = new PopupWindow(popupView, width, height);
            mPopup.showAtLocation(scannerView, Gravity.CENTER, 0, 0);
        } else
            mPopup.dismiss();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_shoppping_cart: {
                Intent intent = new Intent(ScanAndBuy.this, CartActivity.class);
                startActivity(intent);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
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
}
