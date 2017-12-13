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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 13.12.17.
 */

public class ReloadTask extends AsyncTask<Void, Void, List<ListViewItem>> {
    private ProductsViewActivity activity;
    private String refresh_token;
    private String access_token;

    public ReloadTask(ProductsViewActivity activity, String refresh_token, String token) {
        this.activity = activity;
        this.refresh_token = refresh_token;
        this.access_token = token;
    }

    @Override
    protected List<ListViewItem> doInBackground(Void... args) {
        ProductDAO dao = new ProductDAO(this.activity);
        RestClient client = new RestClient();
        String ref_tok = this.refresh_token, tok = this.access_token;
        List<ListViewItem> list = new LinkedList<>();

        dao.truncateProductsTable(); //Usuwam wszystkie elementy

        try {
            RequestResult result = client.get_products(ref_tok, tok);
            tok = Utilities.update_access_token(tok, result.getRefreshedAccessToken());
            JSONObject jo = result.getResponseBodyJSONObject();

            for (ListViewItem item: Utilities.convert_json_to_list(jo)) {
                ProductDBEntitiy product = new ProductDBEntitiy(
                        item.getName(),
                        item.getStoreName(),
                        item.getPrice(),
                        item.getAmount(),
                        0, 0, 0, 0,
                        item.getId());

                dao.addProduct(product);
                list.add(item);
            }
        } catch (InvalidRefreshTokenException ex) {
            Log.e("InvalidRefTokenExc", "Nie udało się wysłąć ");
            return null;
        } catch (JSONException ex) {
            Log.e("JOSNException", "Nie udalo się przetworzyć odpowiedzi z serwera");
            return null;
        }

        this.access_token = tok;

        return list;
    }

    @Override
    protected void onPostExecute(List<ListViewItem> result) {
        if (result == null) {
            Toast.makeText(activity, "Odświeżenie nie powiodło się, spróbuj jeszcze raz", Toast.LENGTH_SHORT).show();
            Log.d("Reload FAILED", "Nie udało się poprawnie wykonać synchronizacji");
        } else {
            activity.setAdapter(result);
            activity.setToken(this.access_token);
            Log.d("Reload SUCCEED", "Odświeżanie przeprowadzono poprawnie :)");
        }
    }
}
