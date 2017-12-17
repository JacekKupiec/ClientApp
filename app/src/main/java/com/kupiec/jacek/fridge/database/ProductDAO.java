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
    private static DBManager db_mng = null;
    private String table_name = "products";
    private String[] columns = { "id", "name", "store_name", "price", "total_amount",
            "subtotal_amount", "remote_id", "new", "removed", "updated", "guid" };

    public ProductDAO(Context ctx) {
        if (db_mng == null)
            db_mng = new DBManager(ctx);
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

    public boolean isThereAnyChanges() {
        String selection = "new = ? OR removed = ? OR updated = ?";
        String[] selectionArgs = { "1", "1", "1" };
        SQLiteDatabase db = db_mng.getReadableDatabase();
        Cursor cursor = db.query(table_name, new String[] { "id" }, selection, selectionArgs, null, null, null);
        boolean result = cursor.moveToFirst();

        db.close();

        return result;
    }

    public List<ProductDBEntitiy> getAllNotRemoved() {
        String selection = "removed = ?";
        String[] selectionArgs = { "0" };

        return get_all_products_that(selection, selectionArgs);
    }

    public List<ProductDBEntitiy> getAllProducts() {
        return get_all_products_that(null, null);
    }

    public void truncateProductsTable() {
        db_mng.getWritableDatabase().execSQL("DELETE FROM products");
    }

    public ProductDBEntitiy getProductById(long id) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(id) };
        ProductDBEntitiy product = null;

        Cursor cursor = db.query(table_name, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst())
            product = new ProductDBEntitiy(cursor);

        db.close();

        return product;
    }

    public ProductDBEntitiy getProductByRemoteId(long remote_id) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        String selection = "remote_id = ?";
        String[] selectionArgs = { String.valueOf(remote_id) };
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

    public long addProduct(ProductDBEntitiy product) {
        ContentValues values = product.getContentValues();
        SQLiteDatabase db = db_mng.getWritableDatabase();

        long id = db.insert(table_name, null, values);

        db.close();

        return id;
    }

    public void updateProduct(ProductDBEntitiy product) {
        update_element_by_id(product);
    }

    public void setAsRemoved(long id) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setRemoved(1);
        update_element_by_id(product);
    }

    public void setAsUpdated(long id) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setUpdated(1);
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

    public void changeSubtotalBy(long id, int delta) {
        ProductDBEntitiy product = get_element_by_id(id);

        product.setSubtotal(product.getSubtotal() + delta);
        update_element_by_id(product);
    }

    public void changeTotalAmountBy(long id, int delta) {
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

    private ProductDBEntitiy get_element_by_id(long id) {
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
