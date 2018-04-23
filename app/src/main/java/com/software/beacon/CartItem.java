package com.software.beacon;


public class CartItem {
    private String description;
    private String original_price;
    private String imageURL;
    private String discounted_price;
    private String discount;
    private int qty = 0;
    private String product_id;


    public CartItem(String head, String price, String imageURL, String discounted, String discount, int qty, String product_id) {
        this.description = head;
        this.original_price = price;
        this.imageURL = imageURL;
        this.discounted_price = discounted;
        this.discount = discount;
        this.qty = qty;
        this.product_id = product_id;
    }

    public String getDescription() {
        return description;
    }

    public String getOriginal_price() {
        return original_price;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getDiscount() {
        return discount;
    }

    public String getDiscounted_price() {
        return discounted_price;
    }

    public int getQty() {
        return qty;
    }

    public String getProduct_id() {
        return product_id;
    }
}
