package com.kupiec.jacek.fridge;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class Utilities {
    @Nullable
    public static Date convert_to_date(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (str == null)
            return null;
        else {
            try {
                return sdf.parse(str);
            } catch (ParseException ex) {
                return null;
            }
        }
    }

    public static boolean is_ref_token_valid(Date ref_token_exp_date) {
        return ref_token_exp_date != null && ref_token_exp_date.compareTo(new Date()) > 0;
    }

    public static List<ListViewItem> convert_json_to_list(JSONObject jo) throws JSONException {
        JSONArray ja = jo.getJSONArray("products");
        List<ListViewItem> list = new LinkedList<>();

        for (int i = 0; i < ja.length(); i++) {
            JSONObject product = ja.getJSONObject(i);
            String name = product.getString("name"),
                    store_name = product.getString("store_name");
            double price = product.getDouble("price");
            int id = product.getInt("id"),
                    amount = product.getInt("amount");

            list.add(new ListViewItem(id, name, store_name, price, amount));
        }

        return list;
    }

    public static String update_access_token(String current_token, String new_token) {
        if (new_token != null && !new_token.isEmpty())
            return new_token;
        else
            return current_token;
    }
}
