package com.software.beacon;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by LENOVO on 14-03-2018.
 */

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder>{

    private final ClickListener listener;
    private final ItemSelectedListener itemSelectedListener;
    public List<CartItem> listItems;
    private Context context;

    public CartAdapter(List<CartItem> listItems, Context context,ClickListener listener, ItemSelectedListener itemSelectedListener) {
        this.listener = listener;
        this.listItems = listItems;
        this.context = context;
        this.itemSelectedListener = itemSelectedListener;
    }

    public List<CartItem> getListItems() {
        return listItems;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        CartItem listItem = listItems.get(position);
        holder.mCardSpinner.setTag(position);

        holder.textViewDescription.setText(listItem.getDescription());
        Picasso.with(context)
                .load(listItem.getImageURL())
                .into(holder.myImage);

        Typeface tahoma_face = Typeface.createFromAsset(context.getAssets(), "tahoma.ttf");
        Typeface tahoma_face_bold = Typeface.createFromAsset(context.getAssets(), "tahoma_bold.ttf");

        holder.textViewDiscountedPrice.setText(listItem.getDiscounted_price());
        holder.textViewDiscountedPrice.setTypeface(tahoma_face_bold);

        holder.textViewOriginalPrice.setText(listItem.getOriginal_price());
        holder.textViewOriginalPrice.setTypeface(tahoma_face);
        holder.textViewOriginalPrice.setPaintFlags(holder.textViewOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        holder.textViewDiscount.setText(listItem.getDiscount());
        holder.textViewDiscount.setTypeface(tahoma_face);

        try {
            Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);
            // Get private mPopup member variable and try cast to ListPopupWindow
            android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(holder.spinner);
            // Set popupWindow height to 500px
            popupWindow.setHeight(500);
        } catch (NoClassDefFoundError | ClassCastException | NoSuchFieldException | IllegalAccessException e) {
            // silently fail...
        }

        final String[] data = new String[50];
        for (int i=0;i<50;i++)
            data[i] = (i+1)+"";

        final ArrayAdapter qty_adapter = new ArrayAdapter<>(context, R.layout.spinner_item_selected, data);
        qty_adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        holder.spinner.setAdapter(qty_adapter);
        holder.spinner.setSelection(listItem.getQty()-1);
        qty_adapter.notifyDataSetChanged();

        holder.spinner.setSpinnerEventsListener(new CustomSpinner.OnSpinnerEventsListener() {
            public void onSpinnerOpened() {
                holder.spinner.setSelected(true);
            }

            public void onSpinnerClosed() {
                holder.spinner.setSelected(false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnItemSelectedListener{

        public TextView textViewDescription, textViewOriginalPrice, textViewDiscountedPrice, textViewDiscount;
        public CustomSpinner spinner;
        public ImageView myImage;
        public AppCompatButton removeButton;
        private WeakReference<ClickListener> listenerRef;
        private WeakReference<ItemSelectedListener> itemSelectedListenerRef;
        public CustomSpinner mCardSpinner;

        public ViewHolder(View itemView) {
            super(itemView);

            listenerRef = new WeakReference<>(listener);
            itemSelectedListenerRef = new WeakReference<>(itemSelectedListener);
            textViewDescription = (TextView) itemView.findViewById(R.id.text_title);
            spinner = (CustomSpinner) itemView.findViewById(R.id.spinner);
            myImage = (ImageView) itemView.findViewById(R.id.image_thumbnail);
            textViewOriginalPrice = (TextView) itemView.findViewById(R.id.text_original_price);
            textViewDiscountedPrice = (TextView) itemView.findViewById(R.id.text_discounted_price);
            textViewDiscount = (TextView) itemView.findViewById(R.id.text_discount);
            removeButton = (AppCompatButton) itemView.findViewById(R.id.btn_remove);
            removeButton.setOnClickListener(this);
            spinner.setOnItemSelectedListener(this);
            mCardSpinner = spinner;
        }

        @Override
        public void onClick(View view) {
            listenerRef.get().onPositionClicked(getAdapterPosition());
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
            int list_position = (int) adapterView.getTag();
            itemSelectedListenerRef.get().onItemClicked(position, Integer.parseInt(spinner.getSelectedItem() + ""),list_position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

}
