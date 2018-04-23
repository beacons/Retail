package com.software.beacon;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class CartActivity extends AppCompatActivity implements ValidationResponse {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private PopupWindow mPopup;
    private List<CartItem> listItems;
    private LinearLayout linearLayout;
    private SessionManager session;
    private int removePosition = 0;
    private double cartTotal = 0.0;
    private double cartDiscount = 0.0;
    private double cartPayable = 0.0;
    private TextView textViewCartTotal, textViewCartDiscount, textViewCartPayable;
    private LinearLayout priceDetailsContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        linearLayout = (LinearLayout) findViewById(R.id.main_content);
        session = new SessionManager(this);
        textViewCartTotal = (TextView) findViewById(R.id.text_cart_total);
        textViewCartDiscount = (TextView) findViewById(R.id.text_cart_discount);
        textViewCartPayable = (TextView) findViewById(R.id.text_cart_payable);
        priceDetailsContainer = (LinearLayout) findViewById(R.id.price_details_container);

        Get_Result conn = new Get_Result(this);
        conn.delegate = CartActivity.this;
        String query = "select * from product left join deals on product.product_id = deals.deals_id where product.product_id IN (select product_id from user_cart where user_id = '" + session.getUserDetails().get(session.KEY_MOB) + "');"
                + "select product_id,quantity from user_cart where user_id = '" + session.getUserDetails().get(session.KEY_MOB) + "'";
        Log.e("dopost", query);
        conn.execute(URLS.Fetch_Cart_URL, query);


        recyclerView = (RecyclerView) findViewById(R.id.cart_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listItems = new ArrayList<>();
        adapter = new CartAdapter(listItems, this, new ClickListener() {
            @Override
            public void onPositionClicked(int position) {
                removePosition = position;
                showProgress(true);
                Get_Result conn = new Get_Result(CartActivity.this);
                conn.delegate = CartActivity.this;
                String query = "delete from user_cart where user_id = '" + session.getUserDetails().get(session.KEY_MOB) + "' and product_id = '" + listItems.get(position).getProduct_id() + "'";
                Log.e("dopost", query);
                conn.execute(URLS.Remove_From_Cart_URL, query);
                if (priceDetailsContainer.getVisibility() == View.VISIBLE)
                    priceDetailsContainer.setVisibility(View.GONE);
            }
        }, new ItemSelectedListener() {
            @Override
            public void onItemClicked(int position, int qty, int list_position) {
                if (listItems.get(list_position).getQty() != qty) {
                    updatePriceDetails();
                    showProgress(true);
                    Get_Result conn = new Get_Result(CartActivity.this);
                    conn.delegate = CartActivity.this;
                    String query = "update user_cart set quantity = " + qty + " where user_id = '" + session.getUserDetails().get(session.KEY_MOB) + "' and product_id = '" + listItems.get(list_position).getProduct_id() + "'";
                    Log.e("dopost", query);
                    conn.execute(URLS.User_Cart, query);
                }
            }
        });
        recyclerView.setAdapter(adapter);
        showProgress(true);


    }

    @Override
    public void response(boolean result, String s) {
        String rs = this.getResources().getString(R.string.Rs);
        if (result) {
            try {
                JSONArray data = new JSONArray(s);
                Log.e("dopost", "response");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject detail = data.getJSONObject(i);
                    double price = detail.getDouble("product_price");
                    double discounted_price = 0.0;
                    int qty = detail.getInt("quantity");
                    Double discount = detail.isNull("discount") ? null : detail.getDouble("discount");
                    String priceDetails[] = {"", "", ""};
                    if (discount != null) {
                        discount = detail.getDouble("discount");
                        discounted_price = Math.round(((100 - discount) / 100) * price);
                        cartDiscount += ((price - discounted_price) * qty);
                        priceDetails[0] = rs + price + "";
                        cartTotal += (price * qty);
                        priceDetails[1] = "(" + discount + "% OFF)";
                        priceDetails[2] = rs + discounted_price;
                    } else {
                        priceDetails[0] = "";
                        priceDetails[1] = "";
                        priceDetails[2] = rs + price + "";
                        cartTotal += (price * qty);
                    }
                    Log.e("Cart", detail.getString("product_name") + " " + priceDetails[0] + " " + detail.getString("product_image_path") + " " + priceDetails[2] + " " + priceDetails[1] + " " + detail.getInt("quantity"));
                    CartItem listItem = new CartItem(detail.getString("product_name"), priceDetails[0], detail.getString("product_image_path"), priceDetails[2], priceDetails[1], detail.getInt("quantity"), detail.getString("product_id"));
                    listItems.add(listItem);
                }
                cartPayable = cartTotal - cartDiscount;
                updatePriceDetails();
                adapter.notifyDataSetChanged();
                showProgress(false);
            } catch (JSONException e) {
                try {
                    showProgress(false);
                    JSONObject jsonObject = new JSONObject(s);
                    if (!jsonObject.getBoolean("error")) {
                        if (jsonObject.getBoolean("deleted")) {
                            listItems.remove(removePosition);
                            adapter.notifyItemRemoved(removePosition);
                            if(listItems.isEmpty())
                                Snackbar.make(linearLayout, Html.fromHtml("<b> Error! Try Again . . .</b>"), Snackbar.LENGTH_INDEFINITE).show();
                        }
                    } else
                        Snackbar.make(linearLayout, Html.fromHtml("<b> Error! Try Again . . .</b>"), Snackbar.LENGTH_INDEFINITE).show();
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        } else {
            showProgress(false);
            String value = (s.equals("None")) ? "Empty Cart !" : s;
            if (!s.equals("ok"))
                Snackbar.make(linearLayout, Html.fromHtml("<b>" + value + "</b>"), Snackbar.LENGTH_INDEFINITE).show();
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
            findViewById(R.id.cart_recycler_view).post(new Runnable() {
                public void run() {
                    mPopup.showAtLocation(findViewById(R.id.cart_recycler_view), Gravity.CENTER, 0, 0);
                }
            });
        } else
            mPopup.dismiss();
    }

    void updatePriceDetails() {
        String rs = this.getResources().getString(R.string.Rs);
        if (cartPayable != 0.0) {
            if (priceDetailsContainer.getVisibility() == View.GONE)
                priceDetailsContainer.setVisibility(View.VISIBLE);
            textViewCartTotal.setText(rs + " " + cartTotal + "");
            textViewCartDiscount.setText("- " + rs + " " + cartDiscount);
            textViewCartPayable.setText(rs + " " + cartPayable);
        }
    }
}