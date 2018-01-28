package com.kupiec.jacek.fridge.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jacek on 10.12.17.
 */

public class DBManager extends SQLiteOpenHelper {
    public DBManager(Context ctx) {
        super(ctx, "produkty.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String stmt = "CREATE TABLE IF NOT EXISTS products (" +
                "id integer primary key autoincrement," +
                "name text," +
                "store_name text," +
                "price real," +
                "total_amount integer," +
                "subtotal_amount integer," +
                "remote_id integer," +
                "new integer," +
                "removed integer," +
                "updated integer," +
                "guid text," +
                "brand text,"+
                "group_id integer default -1);";
        db.execSQL(stmt);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int odlVersion, int newVersion) {
        String stmt = "CREATE TABLE IF NOT EXISTS groups (" +
                "id integer primary key autoincrement" +
                "name text);";
        db.execSQL(stmt);
    }
}
