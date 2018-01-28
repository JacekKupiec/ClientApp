package com.kupiec.jacek.fridge.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 28.01.18.
 */

public class GroupDAO {
    private static DBManager db_mng = null;
    private String table_name = "groups";
    private String[] columns = { "id", "name" };

    public GroupDAO(Context ctx) {
        if (db_mng == null)
            db_mng = new DBManager(ctx);
    }

    public List<GroupDBEntity> getAllGroups() {
        return get_all_products_that(null, null);
    }

    public GroupDBEntity getGroup(long id) {
        return get_element_by_id(id);
    }

    public void removeGroup(int id) {
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.delete(table_name, "id = ?", new String[] { String.valueOf(id) } );
        db.close();
    }

    public void removeGroup(ProductDBEntity product) {
        removeGroup(product.getId());
    }

    public long addGroup(ProductDBEntity product) {
        ContentValues values = product.getContentValues();
        SQLiteDatabase db = db_mng.getWritableDatabase();

        long id = db.insert(table_name, null, values);

        db.close();

        return id;
    }

    private List<GroupDBEntity> get_all_products_that(String selection, String[] selectionArgs) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        Cursor cursor = db.query(table_name, columns, selection, selectionArgs,null, null, null, null);
        List<GroupDBEntity> list = new LinkedList<>();

        while(cursor.moveToNext()) list.add(new GroupDBEntity(cursor));
        db.close();

        return list;
    }

    private GroupDBEntity get_element_by_id(long id) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(id) };
        Cursor cursor = db.query(table_name, columns, selection, selectionArgs, null, null, null, null);

        cursor.moveToFirst();

        GroupDBEntity p =  new GroupDBEntity(cursor);
        db.close();

        return p;
    }

    private void update_element_by_id(GroupDBEntity group) {
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(group.getId()) };
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.update(table_name, group.getContentValues(), selection, selectionArgs);
        db.close();
    }
}
