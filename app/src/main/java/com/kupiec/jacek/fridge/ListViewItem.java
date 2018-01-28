package com.kupiec.jacek.fridge;

import java.io.Serializable;

/**
 * Created by jacek on 15.11.17.
 */

public class ListViewItem implements Serializable {
    private long id;
    private int amount;
    private String name;
    private String store_name;
    private double price;
    private String brand;

    public ListViewItem(long id, String name, String store_name, double price, int amount, String brand) {
        this.id = id;
        this.name = name;
        this.store_name = store_name;
        this.price = price;
        this.amount = amount;
        this.brand = brand;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
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

    public String getBrand() {
        if (this.brand == null)
            return "";
        else
            return this.brand;
    }
}
