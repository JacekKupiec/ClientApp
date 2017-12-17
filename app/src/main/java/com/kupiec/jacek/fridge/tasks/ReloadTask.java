package com.kupiec.jacek.fridge.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.kupiec.jacek.fridge.ListViewItem;
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
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 13.12.17.
 */

public class ReloadTask extends AsyncTask<Void, Void, List<ListViewItem>> {
    private ArrayAdapter<ListViewItem> adapter;
    private String refresh_token;
    private String access_token;
    private ProductDAO dao;

    public ReloadTask(ArrayAdapter<ListViewItem> adapter, String refresh_token, String token, ProductDAO dao) {
        this.adapter = adapter;
        this.refresh_token = refresh_token;
        this.access_token = token;
        this.dao = dao;
    }

    @Override
    protected List<ListViewItem> doInBackground(Void... args) {
        RestClient client = new RestClient();
        String ref_tok = this.refresh_token, tok = this.access_token;
        List<ListViewItem> list = new LinkedList<>();

        //Reload oznacza ,że użytkownik się przelogował, trzeba usunąć ślady poprzedniego
        dao.truncateProductsTable();

        try {
            RequestResult result = client.get_products(ref_tok, tok);
            tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());
            JSONObject jo = result.getResponseBodyJSONObject();
            JSONArray jt = jo.getJSONArray("products");

            for (int i = 0; i < jt.length(); i++) {
                JSONObject item = jt.getJSONObject(i);
                ProductDBEntitiy product = new ProductDBEntitiy(
                        item.getString("name"),
                        item.getString("store_name"),
                        item.getDouble("price"),
                        item.getInt("amount"),
                        0,0, 0, 0, //Tu musi być 0 bo moga juz istnieć inne delty
                        item.getLong("id"),
                        item.getString("guid"));

                dao.addProduct(product);
                list.add(product.toListViewItem());
            }
        } catch (InvalidRefreshTokenException ex) {
            Log.e("InvalidRefTokenExc", "Nie udało się wysłąć ");
            return null;
        } catch (IOException ex) {
            Log.d("IOException", "Brak połączenia z Internetem");
            return Utilities.load_from_db(dao); //Nie ma połączenia z internetem
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udalo się przetworzyć odpowiedzi z serwera");
            return null;
        }

        this.access_token = tok;

        return list;
    }

    @Override
    protected void onPostExecute(List<ListViewItem> result) {
        if (result == null) {
            Log.d("Reload FAILED", "Nie udało się poprawnie wykonać synchronizacji");
        } else {
            this.adapter.clear();
            this.adapter.addAll(result);
            this.adapter.notifyDataSetChanged();
            Log.d("Reload SUCCEED", "Odświeżanie przeprowadzono poprawnie :)");
        }
    }
}
