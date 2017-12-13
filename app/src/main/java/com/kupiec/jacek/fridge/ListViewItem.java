package com.kupiec.jacek.fridge;

import java.io.Serializable;

/**
 * Created by jacek on 15.11.17.
 */

public class ListViewItem implements Serializable {
    private int id;
    private int amount;
    private String name;
    private String store_name;
    private double price;
    private int subtotal;
    private int _new;
    private int removed;
    private int updated;
    private int remote_id;

    public ListViewItem(int id, String name, String store_name, double price, int amount) {
        this.id = id;
        this.name = name;
        this.store_name = store_name;
        this.price = price;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public int getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(int subtotal) {
        this.subtotal = subtotal;
    }

    public int isNew() {
        return _new;
    }

    public void setNew(int _new) {
        this._new = _new;
    }

    public int isRemoved() {
        return removed;
    }

    public void setRemoved(int removed) {
        this.removed = removed;
    }

    public int getRemote_id() {
        return remote_id;
    }

    public int isUpdated() {
        return updated;
    }

    public void setUpdated(int updated) { this.updated = updated; }

    public void setRemote_id(int remote_id) {
        this.remote_id = remote_id;
    }
}
