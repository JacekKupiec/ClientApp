package com.kupiec.jacek.fridge.database;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by jacek on 28.01.18.
 */

public class GroupDBEntity {
    private int id;
    private String name;

    public GroupDBEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public GroupDBEntity(Cursor c) {
        this.id = c.getInt(0);
        this.name = c.getString(1);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put("name", this.name);

        return values;
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
}
