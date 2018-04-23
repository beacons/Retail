package com.software.beacon;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by LENOVO on 14-03-2018.
 */

public class DealsOffersAdapter extends RecyclerView.Adapter<DealsOffersAdapter.ViewHolder>{

    private List<DealsOffersItem> listItems;
    private Context context;

    public DealsOffersAdapter(List<DealsOffersItem> listItems, Context context) {
        this.listItems = listItems;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deals_offers_list_item, parent, false) ;
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DealsOffersItem listItem = listItems.get(position);

        holder.textViewDescription.setText(listItem.getDescription());
        holder.textViewOriginalPrice.setText(listItem.getOriginal_price());

        Picasso.with(context)
                .load(listItem.getImageURL())
                .into(holder.myImage);
        holder.textViewDiscountPrice.setText(listItem.getDiscounted_price());
        holder.textViewOriginalPrice.setPaintFlags(holder.textViewOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        if(listItem.getDiscount().equals("")){
            if(holder.textViewDiscountBadge.getVisibility() == View.VISIBLE)
                holder.textViewDiscountBadge.setVisibility(View.GONE);
        }
        else{
            if(holder.textViewDiscountBadge.getVisibility() == View.GONE)
                holder.textViewDiscountBadge.setVisibility(View.VISIBLE);
        }
        holder.textViewDiscountBadge.setText(listItem.getDiscount());
        holder.textViewValidity.setText(listItem.getValidity());

    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView textViewDescription, textViewOriginalPrice, textViewDiscountPrice;
        public TextView textViewDiscountBadge, textViewValidity;
        public ImageView myImage;

        public ViewHolder(View itemView) {
            super(itemView);

            textViewDescription = (TextView) itemView.findViewById(R.id.text_title);
            textViewOriginalPrice = (TextView) itemView.findViewById(R.id.text_original_price);
            myImage = (ImageView) itemView.findViewById(R.id.image_thumbnail);
            textViewDiscountPrice = (TextView) itemView.findViewById(R.id.text_discount_price);
            textViewDiscountBadge = (TextView) itemView.findViewById(R.id.discount_badge);
            textViewValidity = (TextView) itemView.findViewById(R.id.text_validity);
        }
    }

}
