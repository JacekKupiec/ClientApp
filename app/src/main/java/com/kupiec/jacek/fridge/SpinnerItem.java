package com.kupiec.jacek.fridge;

/**
 * Created by jacek on 28.01.18.
 */

public class SpinnerItem {
    private long remote_id;
    private String name;

    public SpinnerItem(long remote_id, String name) {

    }

    @Override
    public String toString() {
        return getName();
    }

    public long getRemoteId() {
        return remote_id;
    }

    public void setRemoteId(long remote_id) {
        this.remote_id = remote_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
