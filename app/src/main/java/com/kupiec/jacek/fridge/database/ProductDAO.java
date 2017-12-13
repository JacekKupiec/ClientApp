package com.kupiec.jacek.fridge.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.kupiec.jacek.fridge.ListViewItem;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 10.12.17.
 */

public class ProductDAO {
    private DBManager db_mng;
    private String table_name = "products";
    private String[] columns = { "id", "name", "store_name", "price", "total_amount",
            "subtotal_amoount", "remote_id", "new", "removed", "updated" };

    public ProductDAO(Context ctx) {
        this.db_mng = new DBManager(ctx);
    }

    public List<ProductDBEntitiy> getAllNewProducts() {
        String selection = "new = ?";
        String[] selectionArgs = { "1" };

        return get_all_products_that(selection, selectionArgs);
    }

    public List<ProductDBEntitiy> getAllProductsToRemove() {
        String selection = "removed = ?";
        String[] selectionArgs = { "1" };

        return get_all_products_that(selection, selectionArgs);
    }

    public List<ProductDBEntitiy> getAllUpdatedProducts() {
        String selection = "updated = ?";
        String[] selectionArgs = { "1" };

        return get_all_products_that(selection, selectionArgs);
    }

    public List<ProductDBEntitiy> getAllProducts() {
        return get_all_products_that(null, null);
    }

    public void truncateProductsTable() {
        db_mng.getWritableDatabase().execSQL("DELETE FROM products");
    }

    public ProductDBEntitiy getProduct(ListViewItem item) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        String selection = "remote_id = ?";
        String[] selectionArgs = { String.valueOf(item.getId()) };
        ProductDBEntitiy product = null;

        Cursor cursor = db.query(table_name, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst())
            product = new ProductDBEntitiy(cursor);

        db.close();

        return product;
    }

    public void removeProduct(int id) {
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.delete(table_name, "id = ?", new String[] { String.valueOf(id) } );
        db.close();
    }

    public void removeProdukt(ProductDBEntitiy product) {
        removeProduct(product.getId());
    }

    public void addProduct(ProductDBEntitiy product) {
        ContentValues values = product.getContentValues();
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.insert(table_name, null, values);

        db.close();
    }

    public void updateProduct(ProductDBEntitiy product) {
        update_element_by_id(product);
    }

    public void updateTotalAmount(int id, int total) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setTotal(total);
        update_element_by_id(product);
    }

    public void updateSubtotalAmount(int id, int subtotal) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setSubtotal(subtotal);
        update_element_by_id(product);
    }

    public void changeSubtotalBy(int id, int delta) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setSubtotal(product.getSubtotal() + delta);
        update_element_by_id(product);
    }

    public void changeTotalAmountBy(int id, int delta) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setTotal(product.getTotal() + delta);
        update_element_by_id(product);
    }

    private List<ProductDBEntitiy> get_all_products_that(String selection, String[] selectionArgs) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        Cursor cursor = db.query(table_name, columns, selection, selectionArgs,null, null, null, null);
        List<ProductDBEntitiy> list = new LinkedList<>();

        while(cursor.moveToNext()) list.add(new ProductDBEntitiy(cursor));
        db.close();

        return list;
    }

    private ProductDBEntitiy get_element_by_id(int id) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(id) };
        Cursor cursor = db.query(table_name, columns, selection, selectionArgs, null, null, null, null);

        cursor.moveToFirst();

        ProductDBEntitiy p =  new ProductDBEntitiy(cursor);
        db.close();

        return p;
    }

    private void update_element_by_id(ProductDBEntitiy product) {
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(product.getId()) };
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.update(table_name, product.getContentValues(), selection, selectionArgs);
        db.close();
    }
}
