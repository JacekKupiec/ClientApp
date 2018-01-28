package com.kupiec.jacek.fridge.database;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by jacek on 28.01.18.
 */

public class GroupDBEntity {
    private int id;
    private String name;
    private long remote_id;

    public GroupDBEntity(String name, long remote_id) {
        this.name = name;
        this.remote_id = remote_id;
    }

    public GroupDBEntity(Cursor c) {
        this.id = c.getInt(0);
        this.name = c.getString(1);
        this.remote_id = c.getInt(2);
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();

        values.put("name", this.name);
        values.put("remote_id", this.remote_id);

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

    public long getRemoteId() { return remote_id; }
}
