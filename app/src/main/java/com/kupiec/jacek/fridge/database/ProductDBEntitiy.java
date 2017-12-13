package com.kupiec.jacek.fridge.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.kupiec.jacek.fridge.net.ProductNet;

/**
 * Created by jacek on 10.12.17.
 */

public class ProductDBEntitiy {
    private int id;
    private String name;
    private String store_name;
    private double price;
    private int total;
    private int subtotal;
    private int _new;
    private int removed;
    private int updated;
    private int remote_id;

    public ProductDBEntitiy(int id, String name, String store_name, double price, int subtotal, int _new, int removed, int updated, int remote_id) {
        this.id = id;
        this.name = name;
        this.store_name = store_name;
        this.price = price;
        this.subtotal = subtotal;
        this._new = _new;
        this.removed = removed;
        this.updated = updated;
        this.remote_id = remote_id;
    }

    public ProductDBEntitiy(String name, String store_name, double price, int total, int subtotal, int _new, int removed, int updated, int remote_id) {
        this.name = name;
        this.store_name = store_name;
        this.price = price;
        this.total = total;
        this.subtotal = subtotal;
        this._new = _new;
        this.removed = removed;
        this.updated = updated;
        this.remote_id = remote_id;
    }

    public ProductDBEntitiy(Cursor c) {
        this.id = c.getInt(0);
        this.name = c.getString(1);
        this.store_name = c.getString(2);
        this.price = c.getDouble(3);
        this.total = c.getInt(4);
        this.subtotal = c.getInt(5);
        this.remote_id = c.getInt(6);
        this._new = c.getInt(7);
        this.removed = c.getInt(8);
        this.updated = c.getInt(9);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put("name", this.name);
        values.put("stor_name", this.store_name);
        values.put("price", this.price);
        values.put("total_amount", this.total);
        values.put("subtotal_amount", this.subtotal);
        values.put("remote_id", this.remote_id);
        values.put("new", this._new);
        values.put("removed", this.removed);
        values.put("updated", this.updated);

        return values;
    }

    public ProductNet toProductNet() {
        return new ProductNet(this.name, this.store_name, this.price, this.total);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int isUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getRemoteId() {
        return remote_id;
    }

    public void setRemoteId(int remote_id) {
        this.remote_id = remote_id;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
