package com.kupiec.jacek.fridge.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by jacek on 14.11.17.
 */

public class RequestResult implements Serializable {
    private String response_body;
    private int response_code;
    private String access_token;

    public RequestResult(String response_body, int response_code) {
        this.response_body = response_body;
        this.response_code = response_code;
    }

    public RequestResult(String response_body, int response_code, String access_token) {
        this.response_body = response_body;
        this.response_code = response_code;
        this.access_token = access_token;
    }

    public int getResponseCode() {
        return this.response_code;
    }

    public String getResponseBodyText() {
        return this.response_body;
    }

    public JSONObject getResponseBodyJSONObject() throws JSONException {
        return new JSONObject(this.response_body);
    }

    public String getRefreshedAccessToken() {
        if (this.access_token == null)
            return "";
        else
            return this.access_token;
    }

    public void setRefreshedAccessToken(String str) {
        this.access_token = str;
    }
}
