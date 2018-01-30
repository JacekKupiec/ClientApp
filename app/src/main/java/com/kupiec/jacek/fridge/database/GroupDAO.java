package com.kupiec.jacek.fridge.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 28.01.18.
 */

public class GroupDAO {
    private static DBManager db_mng = null;
    private String table_name = "groups";
    private String[] columns = { "id", "name", "remote_id" };

    public GroupDAO(Context ctx) {
        if (db_mng == null)
            db_mng = new DBManager(ctx);
    }

    public List<GroupDBEntity> getAllGroups() {
        return get_all_groups_that(null, null);
    }

    public GroupDBEntity getGroup(long id) {
        return get_element_by_id(id);
    }

    @Nullable
    public GroupDBEntity getGroupByRemoteId(long id) {
        SQLiteDatabase db = db_mng.getReadableDatabase();
        String selection = "remote_id = ?";
        String[] selection_args = new String[] { String.valueOf(id) };
        Cursor cursor = db.query(table_name, columns, selection, selection_args, null, null, null);
        GroupDBEntity group = null;

        if (cursor.moveToFirst())
             group = new GroupDBEntity(cursor);

        db.close();

        return group;
    }

    public void removeGroup(int id) {
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.delete(table_name, "id = ?", new String[] { String.valueOf(id) } );
        db.close();
    }

    public void removeGroupByRemoteId(long id) {
        SQLiteDatabase db = db_mng.getWritableDatabase();

        db.delete(table_name, "remote_id = ?", new String[] { String.valueOf(id) } );
        db.close();
    }

    public void removeGroup(GroupDBEntity group) {
        removeGroup(group.getId());
    }

    public long addGroup(GroupDBEntity group) {
        ContentValues values = group.getContentValues();
        SQLiteDatabase db = db_mng.getWritableDatabase();

        long id = db.insert(table_name, null, values);

        db.close();

        return id;
    }

    private List<GroupDBEntity> get_all_groups_that(String selection, String[] selectionArgs) {
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

    public void truncateGroupsTable() {
        db_mng.getWritableDatabase().execSQL("DELETE FROM groups");
    }
}
