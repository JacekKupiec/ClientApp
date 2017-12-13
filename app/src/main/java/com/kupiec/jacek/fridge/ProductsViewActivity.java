package com.kupiec.jacek.fridge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.kupiec.jacek.fridge.net.*;
import com.kupiec.jacek.fridge.tasks.SyncTask;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class ProductsViewActivity extends AppCompatActivity {
    /* Jeżeli mój refresh token jest unieważnony to przerywam bierzącą akcję użytkownika i każę
    mu się jeszcze raz zalogować */

    public static final int LOG_IN_ACTIVITY = 1;
    public static final int ADD_PRODUCT_ACTIVITY = 2;
    public static final int SHOW_PRODUCT_ACTIVITY = 3;
    public static final int PRODUCT_REMOVED = 4;
    public static final int PRODUCT_MODIFIED = 5;

    private String token = null;
    private ArrayAdapter<ListViewItem> adapter = null;
    private RestClient client = new RestClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_view);
        Resources r = getResources();

        ListView listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(get_list_view_listener());

        launch_sync_task();
    }

    public void onClickAddButton(View view) {
        Resources r = getResources();
        Intent intent = new Intent(this, CreateProductActivity.class);
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        Date ref_token_exp_date = Utilities.convert_to_date(sp.getString(r.getString(R.string.refresh_token_expiration_date), ""));

        if (Utilities.is_ref_token_valid(ref_token_exp_date)) {
            intent.putExtra(r.getString(R.string.token), this.token);
            startActivityForResult(intent, ADD_PRODUCT_ACTIVITY);
        }
        else {
            Toast.makeText(this, "Na początku zaloguj się lub utwórz konto", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickLogInIndexButton(View view) {
        Intent intent = new Intent(this, LogInActivity.class);

        startActivityForResult(intent, LOG_IN_ACTIVITY);
    }

    public void onClickSyncButton(View view) {
        launch_sync_task();
    }

    public synchronized void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return this.token;
    }

    public synchronized void setAdapter(List<ListViewItem> list) {
        ListView listView = findViewById(R.id.listView);

        this.adapter = new ArrayAdapter<>(this, R.layout.list_item, list);
        listView.setAdapter(this.adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String new_token;

        switch (requestCode) {
            case ADD_PRODUCT_ACTIVITY:
                new_token = data.getStringExtra(r.getString(R.string.token));
                this.token = Utilities.update_access_token(this.token, new_token);

                if (resultCode == Activity.RESULT_OK) {
                    boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload), false);

                    if (should_reload)
                        launch_sync_task();//reload_list_view(sp, data, r);
                    else
                        this.adapter.add((ListViewItem) data.getSerializableExtra(r.getString(R.string.product)));
                }

                break;
            case LOG_IN_ACTIVITY:
                if (resultCode == Activity.RESULT_OK)
                    launch_sync_task();//reload_list_view(sp, data, r);

                break;
            case SHOW_PRODUCT_ACTIVITY:
                new_token = data.getStringExtra(r.getString(R.string.token));
                this.token = Utilities.update_access_token(this.token, new_token);

                if (resultCode == Activity.RESULT_OK) {
                    ListView lv = findViewById(R.id.listView);
                    int product_state = data.getIntExtra(r.getString(R.string.product_status), -1);
                    int position = data.getIntExtra(r.getString(R.string.position),-1);
                    ListViewItem item = (ListViewItem)lv.getItemAtPosition(position);
                    boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload), false);

                    if (should_reload)
                        launch_sync_task();//reload_list_view(sp, data, r);
                    else if (product_state == PRODUCT_REMOVED) {
                        this.adapter.remove(item);
                    } else if (product_state == PRODUCT_MODIFIED) {
                        RequestResult result;
                        String refresh_token = sp.getString(r.getString(R.string.refresh_token), "");

                        try {
                            result = this.client.get_product(refresh_token, this.token, item.getId());
                            item.setAmount(result.getResponseBodyJSONObject().getInt(r.getString(R.string.product_amount)));
                        } catch (InvalidRefreshTokenException exp) {
                            Log.e("InvalidRefreshTokenExc", "Nie dalo się pobrać produktu z serwera");
                            Toast.makeText(this, "Zaloguj sie ponownie aby doświeżyc listę", Toast.LENGTH_SHORT).show();
                        } catch (JSONException ex) {
                            Log.e("JSONException", "Nie udało się przetworzyć odpowiedzi z serwera");
                        }
                    }
                }

                break;
        }
    }

    private AdapterView.OnItemClickListener get_list_view_listener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ListView lv = findViewById(R.id.listView);
                ListViewItem item = (ListViewItem)lv.getItemAtPosition(position);
                Intent intent = new Intent(ProductsViewActivity.this, ProductViewActivity.class);
                Resources r = getResources();

                intent.putExtra(r.getString(R.string.product), item);
                intent.putExtra(r.getString(R.string.token), ProductsViewActivity.this.token);
                intent.putExtra(r.getString(R.string.position), position);
                startActivityForResult(intent, SHOW_PRODUCT_ACTIVITY);
            }
        };
    }

    private void reload_list_view(SharedPreferences sp, Intent data, Resources r) {
        List<ListViewItem> list;
        RequestResult result;
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), "");

        this.token = data.getStringExtra(r.getString(R.string.token));

        try {
            result = this.client.get_products(refresh_token, this.token);
            list = Utilities.convert_json_to_list(result.getResponseBodyJSONObject());
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udało się przetworzyć odpowiedzi z serwera");
            return;
        } catch (InvalidRefreshTokenException ex) {
            Log.e("InvalidRefreshTokenExc","Nie ważny refresh_token po zalogowaniu, coś tu jest nie tak");
            return;
        }

        setAdapter(list);
    }

    private void launch_sync_task() {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String ref_token = sp.getString(r.getString(R.string.refresh_token), "");
        Date ref_token_exp_date = Utilities.convert_to_date(sp.getString(r.getString(R.string.refresh_token_expiration_date), null));

        if (Utilities.is_ref_token_valid(ref_token_exp_date)) {
            try {
                RequestResult result = client.refresh(ref_token);
                JSONObject jo = result.getResponseBodyJSONObject();
                SyncTask task = new SyncTask(this, ref_token, jo.getString(r.getString(R.string.token)));

                task.execute();
            } catch (JSONException ex) {
                Log.e("JSONException", "Nie udało się sprarsować odpowiedzi z serwera :(");
            }
        }
        else
            Toast.makeText(this, "Zaloguj się lub utwórz konto", Toast.LENGTH_SHORT).show();
    }
}