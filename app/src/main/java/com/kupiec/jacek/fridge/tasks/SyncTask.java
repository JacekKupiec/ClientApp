package com.kupiec.jacek.fridge.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.kupiec.jacek.fridge.ListViewItem;
import com.kupiec.jacek.fridge.ProductsViewActivity;
import com.kupiec.jacek.fridge.Utilities;
import com.kupiec.jacek.fridge.database.ProductDAO;
import com.kupiec.jacek.fridge.database.ProductDBEntitiy;
import com.kupiec.jacek.fridge.net.InvalidRefreshTokenException;
import com.kupiec.jacek.fridge.net.RequestResult;
import com.kupiec.jacek.fridge.net.RestClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 10.12.17.
 */


public class SyncTask extends AsyncTask<Void, Void, List<ListViewItem>> {
    private ProductsViewActivity activity;
    private String refresh_token;
    private String access_token;
    private ProductDAO dao;

    public SyncTask(ProductsViewActivity actvity, String refresh_token, String token) {
        this.activity = activity;
        this.refresh_token = refresh_token;
        this.access_token = token;
        this.dao = new ProductDAO(this.activity.getApplicationContext());
    }

    @Override
    protected List<ListViewItem> doInBackground(Void... args) {

        RestClient client = new RestClient();
        String ref_tok = this.refresh_token, tok = this.access_token;


        for (ProductDBEntitiy product: this.dao.getAllNewProducts()) {
            try {
                RequestResult result = client.add_product(ref_tok, tok, product.toProductNet());
                JSONObject jo = result.getResponseBodyJSONObject();
                tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());

                product.setNew(0);
                product.setRemoteId(jo.getInt("id"));
                this.dao.updateProduct(product);
            } catch (InvalidRefreshTokenException ex) {
                Log.e("InvalidRefershToken",
                   "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
                return null;
            } catch (IOException ex) {
                Log.d("IOException", "Brak połączenia z Internetem");
                return null; //Nie ma połączenia z internetem
            } catch (JSONException ex) {
                Log.e("JSONException", "Nieprawidłowy format odpowiedzi z serwera");
                return Utilities.load_from_db(this.dao);
            }
        }

        for (ProductDBEntitiy product: this.dao.getAllProductsToRemove()) {
            try {
                RequestResult result = client.delete_product(ref_tok, tok, product.getRemoteId());
                tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());

                if (result.getResponseCode() == HttpURLConnection.HTTP_OK)
                    this.dao.removeProduct(product.getId());
            } catch (InvalidRefreshTokenException ex) {
                Log.e("InvalidRefershToken",
                        "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
                return null;
            } catch (IOException ex) {
                Log.d("IOException", "Brak połączenia z Internetem");
                return Utilities.load_from_db(this.dao); //Nie ma połączenia z internetem
            }
        }

        try {
            RequestResult result = client.get_products(ref_tok, tok);
            JSONObject jo = result.getResponseBodyJSONObject();
            tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());
            JSONArray jt = jo.getJSONArray("products");

            for (int i = 0; i < jt.length(); i++) {
                JSONObject item = jt.getJSONObject(i);
                ProductDBEntitiy product = this.dao.getProductByRemoteId(item.getLong("id"));

                if (product == null) {
                    ProductDBEntitiy new_product = new ProductDBEntitiy(
                        item.getString("name"),
                        item.getString("store_name"),
                        item.getDouble("price"),
                        item.getInt("amount"),
                        0, 0, 0, 0,
                        item.getLong("id"),
                        item.getString("guid")
                    );

                    this.dao.addProduct(new_product);
                }
                else {
                    this.dao.updateTotalAmount(product.getId(), item.getInt("amount"));
                }
            }
        } catch (InvalidRefreshTokenException ex) {
            Log.e("InvalidRefershToken",
                    "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
            return null;
        } catch (IOException ex) {
            Log.d("IOException", "Brak połączenia z Internetem");
            return Utilities.load_from_db(this.dao); //Nie ma połączenia z internetem
        } catch (JSONException ex) {
            Log.e("JSONException", "Nieprawidłowy format odpowiedzi z serwera");
            return null;
        }

        List<ListViewItem> list = new LinkedList<>();

        for (ProductDBEntitiy product: this.dao.getAllProducts()) {
            list.add(product.toListViewItem());
        }

        this.access_token = tok;

        return list;
    }

    @Override
    protected void onPostExecute(List<ListViewItem> result) {
        if (result == null) {
            Toast.makeText(activity, "Synchronizacja nie powiodła się, spróbuj jeszcze raz", Toast.LENGTH_SHORT).show();
            Log.d("Synchronization FAILED", "Nie udało się poprawnie wykonać synchronizacji");
        } else {
            activity.setAdapter(result);
            activity.setToken(this.access_token);
            Log.d("Synchronization SUCCEED", "Synchronizację przeprowadzono poprawnie :)");
        }
    }
}


