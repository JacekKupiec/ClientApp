package com.kupiec.jacek.fridge.net;

import com.kupiec.jacek.fridge.ListViewItem;

/**
 * Created by jacek on 15.11.17.
 */

public class ProductNet {
    private String name;
    private String store_name;
    private double price;
    private int amount;
    private String guid;
    private String brand;
    private long group_id;

    public ProductNet(String name, String store_name, double price, int amount, String GUID, String brand, long group_id) {
        this.name = name;
        this.store_name = store_name;
        this.price = price;
        this.amount = amount;
        this.guid = GUID;
        this.brand = brand;
        this.group_id = group_id;
    }

    public ListViewItem toListViewItem(long id) {
        return new ListViewItem(id,
                this.name,
                this.store_name,
                this.price,
                this.amount,
                this.brand);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStoreName() {
        return store_name;
    }

    public void setStoreName(String store_name) {
        this.store_name = store_name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getGUID() {
        return guid;
    }

    public String getBrand() { return this.brand; }

    public long getGroupId() {
        return group_id;
    }

    public void setGroupId(long group_id) {
        this.group_id = group_id;
    }
}
