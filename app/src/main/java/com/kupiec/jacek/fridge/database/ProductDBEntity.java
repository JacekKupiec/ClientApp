package com.kupiec.jacek.fridge.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.kupiec.jacek.fridge.ListViewItem;
import com.kupiec.jacek.fridge.net.ProductNet;

/**
 * Created by jacek on 10.12.17.
 */

public class ProductDBEntity {
    private int id;
    private String name;
    private String store_name;
    private double price;
    private int total;
    private int subtotal;
    private int _new;
    private int removed;
    private int updated;
    private long remote_id;
    private String guid;
    private String brand;
    private long group_id;
    private long groupId;

    public ProductDBEntity(String name, String store_name, double price, int total, int subtotal,
                           int _new, int removed, int updated, long remote_id, String guid,
                           String brand, long group_id) {
        this.name = name;
        this.store_name = store_name;
        this.price = price;
        this.total = total;
        this.subtotal = subtotal;
        this._new = _new;
        this.removed = removed;
        this.updated = updated;
        this.remote_id = remote_id;
        this.guid = guid;
        this.brand = brand;
        this.group_id = group_id;
    }

    public ProductDBEntity(Cursor c) {
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
        this.guid = c.getString(10);
        this.brand = c.getString(11);
        this.group_id = c.getInt(12);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put("name", this.name);
        values.put("store_name", this.store_name);
        values.put("price", this.price);
        values.put("total_amount", this.total);
        values.put("subtotal_amount", this.subtotal);
        values.put("remote_id", this.remote_id);
        values.put("new", this._new);
        values.put("removed", this.removed);
        values.put("updated", this.updated);
        values.put("guid", this.guid);
        values.put("brand", this.brand);
        values.put("group_id", this.group_id);

        return values;
    }

    public ProductNet toProductNet() {
        return new ProductNet(this.name,
                this.store_name,
                this.price,
                this.total,
                this.guid,
                this.brand,
                this.group_id);
    }

    public ListViewItem toListViewItem(long id) {
        return new ListViewItem(id,
                getName(),
                getStoreName(),
                getPrice(),
                getTotal(),
                getBrand());
    }

    public ListViewItem toListViewItem() {
        return new ListViewItem(getId(),
                getName(),
                getStoreName(),
                getPrice(),
                getTotal(),
                getBrand());
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

    public long getRemoteId() {
        return remote_id;
    }

    public void setRemoteId(long remote_id) {
        this.remote_id = remote_id;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getGUID() {
        return this.guid;
    }

    public String getBrand() { return this.brand; }

    public long getGroupId() { return this.group_id; }

    public void setGroupId(long group_id) {
        this.group_id = group_id;
    }
}
