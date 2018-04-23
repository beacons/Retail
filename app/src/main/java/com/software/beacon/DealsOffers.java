package com.software.beacon;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DealsOffers extends AppCompatActivity implements ValidationResponse {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private PopupWindow mPopup;
    private List<DealsOffersItem> listItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deals_offers);

        Get_Result conn = new Get_Result(this);
        conn.delegate = DealsOffers.this;
        String query = "select * from product left JOIN deals on product.product_id = deals.deals_id";
        conn.execute(URLS.Fetch_Product_URL, query);


        recyclerView = (RecyclerView) findViewById(R.id.deals_offers_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listItems = new ArrayList<>();
        adapter = new DealsOffersAdapter(listItems, this);
        recyclerView.setAdapter(adapter);

        showProgress(true);

/*        //ImageView iv = (ImageView) findViewById(R.drawable.ic_600978763077);
        //Drawable d = ContextCompat.getDrawable(this, R.drawable.ic_600978763077);
        //BitmapDrawable bitDw = ((BitmapDrawable) d);
        //Bitmap bitmap = bitDw.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        System.out.println("........length......" + imageInByte);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap image = BitmapFactory.decodeStream(bis, null, options);*/


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
                    String priceDetails[] = {"", "", "", ""};
                    if (discount != null) {
                        discount = detail.getDouble("discount");
                        discounted_price = Math.round(((100 - discount) / 100) * price);
                        priceDetails[0] = rs + price + "";
                        priceDetails[1] = discount + "%\nOFF";
                        priceDetails[2] = rs + +discounted_price;
                        priceDetails[3] = validTill + getDate(detail.getString("end_date"));
                        DealsOffersItem listItem = new DealsOffersItem(detail.getString("product_name"), priceDetails[0], detail.getString("product_image_path"), priceDetails[2], priceDetails[1], priceDetails[3]);
                        listItems.add(listItem);
                    }
                }
                adapter.notifyDataSetChanged();
                showProgress(false);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            showProgress(false);
            String value = (s.equals("None")) ? "No Result !" : s;
            Snackbar.make(findViewById(R.id.deals_offers), Html.fromHtml("<b>" + value + "</b>"), Snackbar.LENGTH_INDEFINITE).show();
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

    public void showProgress(final boolean show) {
        View popupView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.my_progress, null);
        if (show) {
            Point size = new Point();
            this.getWindowManager().getDefaultDisplay().getSize(size);
            int width = size.x;
            int height = size.y;
            mPopup = new PopupWindow(popupView, width, height);
            //mPopup.showAtLocation(findViewById(R.id.deals_offers), Gravity.CENTER, 0, 0);
            findViewById(R.id.deals_offers).post(new Runnable() {
                public void run() {
                    mPopup.showAtLocation(findViewById(R.id.deals_offers), Gravity.CENTER, 0, 0);
                }
            });
        } else
            mPopup.dismiss();
    }
}