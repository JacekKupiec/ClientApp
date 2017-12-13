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
                "id int primary key autoincrement," +
                "name text," +
                "store_name text," +
                "price real," +
                "total_amount int," +
                "subtotal_amount int," +
                "remote_id int," +
                "new int," +
                "removed int," +
                "updated int);";
        db.execSQL(stmt);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int odlVersion, int newVersion) {

    }
}
