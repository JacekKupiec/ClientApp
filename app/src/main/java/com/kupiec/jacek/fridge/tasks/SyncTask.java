package com.kupiec.jacek.fridge.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.kupiec.jacek.fridge.ListViewItem;
import com.kupiec.jacek.fridge.Utilities;
import com.kupiec.jacek.fridge.database.ProductDAO;
import com.kupiec.jacek.fridge.database.ProductDBEntity;
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
    private ArrayAdapter<ListViewItem> productAdapter;
    private String refresh_token;
    private String access_token;
    private ProductDAO productDAO;

    public SyncTask(ArrayAdapter<ListViewItem> productAdapter, String refresh_token, String token, ProductDAO productDAO) {
        this.productAdapter = productAdapter;
        this.refresh_token = refresh_token;
        this.access_token = token;
        this.productDAO = productDAO;
    }

    @Override
    protected List<ListViewItem> doInBackground(Void... args) {

        RestClient client = new RestClient();
        String ref_tok = this.refresh_token, tok = this.access_token;


        for (ProductDBEntity product: this.productDAO.getAllNewProducts()) {
            try {
                RequestResult result = client.add_product(ref_tok, tok, product.toProductNet());
                JSONObject jo = result.getResponseBodyJSONObject();
                tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());

                product.setNew(0);
                product.setRemoteId(jo.getInt("id"));
                this.productDAO.updateProduct(product);
            } catch (InvalidRefreshTokenException ex) {
                Log.e("InvalidRefershToken",
                   "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
                return null;
            } catch (IOException ex) {
                Log.d("IOException", "Brak połączenia z Internetem");
                return null; //Nie ma połączenia z internetem
            } catch (JSONException ex) {
                Log.e("JSONException", "Nieprawidłowy format odpowiedzi z serwera");
                return Utilities.load_from_db(this.productDAO);
            }
        }

        for (ProductDBEntity product: this.productDAO.getAllProductsToRemove()) {
            try {
                RequestResult result = client.delete_product(ref_tok, tok, product.getRemoteId());
                tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());

                if (result.getResponseCode() == HttpURLConnection.HTTP_OK)
                    this.productDAO.removeProduct(product.getId());
            } catch (InvalidRefreshTokenException ex) {
                Log.e("InvalidRefershToken",
                        "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
                return null;
            } catch (IOException ex) {
                Log.d("IOException", "Brak połączenia z Internetem");
                return Utilities.load_from_db(this.productDAO); //Nie ma połączenia z internetem
            }
        }

        try {
            RequestResult result = client.get_products(ref_tok, tok);
            JSONObject jo = result.getResponseBodyJSONObject();
            tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());
            JSONArray jt = jo.getJSONArray("products");
            LinkedList<Long> to_not_delete = new LinkedList<>();

            for (int i = 0; i < jt.length(); i++) {
                JSONObject item = jt.getJSONObject(i);
                ProductDBEntity product = this.productDAO.getProductByRemoteId(item.getLong("id"));

                if (product == null) {
                    ProductDBEntity new_product = new ProductDBEntity(
                        item.getString("name"),
                        item.getString("store_name"),
                        item.getDouble("price"),
                        item.getInt("amount"),
                        0, 0, 0, 0, //Tu subtotal na 0 bo istnieja już inne delty
                        item.getLong("id"),
                        item.getString("guid"),
                        item.getString("brand"),
                        item.getInt("group_id")
                    );

                    long id = this.productDAO.addProduct(new_product);
                    to_not_delete.add(id);
                }
                else {
                    result = client.sync_subsum(ref_tok, tok, product.getRemoteId(), product.getSubtotal());

                    if (result.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONObject jj = result.getResponseBodyJSONObject();

                        product.setTotal(jj.getInt("amount"));
                        product.setUpdated(0);
                        this.productDAO.updateProduct(product);
                        to_not_delete.add((long)product.getId());
                    }
                }
            }

            productDAO.deleteAllThatNotIn(to_not_delete.toArray(new Long[to_not_delete.size()]));
        } catch (InvalidRefreshTokenException ex) {
            Log.e("InvalidRefershToken",
                    "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
            return null;
        } catch (IOException ex) {
            Log.d("IOException", "Brak połączenia z Internetem");
            return Utilities.load_from_db(this.productDAO); //Nie ma połączenia z internetem
        } catch (JSONException ex) {
            Log.e("JSONException", "Nieprawidłowy format odpowiedzi z serwera");
            return null;
        }

        List<ListViewItem> list = new LinkedList<>();

        for (ProductDBEntity product: this.productDAO.getAllProducts()) {
            list.add(product.toListViewItem());
        }

        this.access_token = tok;

        return list;
    }

    @Override
    protected void onPostExecute(List<ListViewItem> result) {
        if (result == null) {
            Log.d("Synchronization FAILED", "Nie udało się poprawnie wykonać synchronizacji");
        } else {
            this.productAdapter.clear();
            this.productAdapter.addAll(result);
            this.productAdapter.notifyDataSetChanged();
            Log.d("Synchronization SUCCEED", "Synchronizację przeprowadzono poprawnie :)");
        }
    }
}


