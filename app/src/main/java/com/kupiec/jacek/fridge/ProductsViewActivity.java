package com.kupiec.jacek.fridge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.kupiec.jacek.fridge.database.GroupDAO;
import com.kupiec.jacek.fridge.database.ProductDAO;
import com.kupiec.jacek.fridge.database.ProductDBEntity;
import com.kupiec.jacek.fridge.net.*;
import com.kupiec.jacek.fridge.tasks.DownloadGroupsTask;
import com.kupiec.jacek.fridge.tasks.ReloadTask;
import com.kupiec.jacek.fridge.tasks.SyncTask;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ProductsViewActivity extends AppCompatActivity {
    /* Jeżeli mój refresh token jest unieważnony to przerywam bierzącą akcję użytkownika i każę
    mu się jeszcze raz zalogować */

    public static final int LOG_IN_ACTIVITY = 1;
    public static final int ADD_PRODUCT_ACTIVITY = 2;
    public static final int SHOW_PRODUCT_ACTIVITY = 3;
    public static final int PRODUCT_REMOVED = 4;
    public static final int PRODUCT_MODIFIED = 5;
    public static final int GROUPS_ACTIVITY = 6;

    private String token = null;
    private ArrayAdapter<ListViewItem> productAdapter;
    private ArrayAdapter<SpinnerItem> groupAdapter;
    private RestClient client = new RestClient();
    private ProductDAO productDAO;
    private GroupDAO groupDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_view);

        this.productDAO = new ProductDAO(getApplicationContext());
        this.groupDAO = new GroupDAO(getApplicationContext());
        this.productAdapter = new ArrayAdapter<>(this, R.layout.list_item);
        this.groupAdapter = new ArrayAdapter<SpinnerItem>(this, R.layout.list_item);

        ListView listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(get_list_view_listener());
        listView.setAdapter(this.productAdapter);

        Spinner spinner = findViewById(R.id.chooseGroupSpinner);
        spinner.setAdapter(groupAdapter);
        spinner.setOnItemClickListener(get_spinner_listener());

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


    public void onClickGroupsButton(View view) {
        Resources r = getResources();
        Intent intent = new Intent(this, GroupsActivity.class);
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        Date ref_token_exp_date = Utilities.convert_to_date(sp.getString(r.getString(R.string.refresh_token_expiration_date), ""));

        if (Utilities.is_ref_token_valid(ref_token_exp_date)) {
            intent.putExtra(r.getString(R.string.token), this.token);
            startActivityForResult(intent, GROUPS_ACTIVITY);
        }
        else {
            Toast.makeText(this, "Na początku zaloguj się lub utwórz konto", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Resources r = getResources();
        String new_token;

        switch (requestCode) {
            case ADD_PRODUCT_ACTIVITY:
                new_token = data.getStringExtra(r.getString(R.string.token));
                this.token = Utilities.update_access_token(this.token, new_token);

                if (resultCode == Activity.RESULT_OK) {
                    boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload), false);

                    if (should_reload)
                        launch_reload_task();
                    else
                        this.productAdapter.add((ListViewItem) data.getSerializableExtra(r.getString(R.string.product)));
                }

                break;
            case LOG_IN_ACTIVITY:
                if (resultCode == Activity.RESULT_OK) {
                    boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload), false);

                    if (should_reload) launch_reload_task();
                }

                break;
            case SHOW_PRODUCT_ACTIVITY:
                new_token = data.getStringExtra(r.getString(R.string.token));
                this.token = Utilities.update_access_token(this.token, new_token);

                if (resultCode == Activity.RESULT_OK) {
                    int product_state = data.getIntExtra(r.getString(R.string.product_status), -1);
                    int position = data.getIntExtra(r.getString(R.string.position),-1);
                    boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload), false);
                    ListView lv = findViewById(R.id.listView);
                    ListViewItem item = (ListViewItem)lv.getItemAtPosition(position);

                    if (should_reload)
                        launch_reload_task();
                    else if (product_state == PRODUCT_REMOVED)
                        this.productAdapter.remove(item);
                    else if (product_state == PRODUCT_MODIFIED) {
                        ProductDBEntity product = this.productDAO.getProductById(item.getId());

                        item.setAmount(product.getTotal());
                    }
                }

                break;
            case GROUPS_ACTIVITY:
                new_token = data.getStringExtra(r.getString(R.string.token));
                this.token = Utilities.update_access_token(this.token, new_token);

                if (resultCode == Activity.RESULT_OK) {
                    boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload), false);

                    if (should_reload)
                        launch_reload_task();
                    else
                        launch_download_groups_task();
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


    private AdapterView.OnItemClickListener get_spinner_listener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Spinner chooseGroupSpinner = findViewById(R.id.chooseGroupSpinner);
                SpinnerItem item = (SpinnerItem) chooseGroupSpinner.getItemAtPosition(position);

                if (item.getRemoteId() == 0) {
                    List<ListViewItem> list = Utilities.load_from_db(ProductsViewActivity.this.productDAO);

                    ProductsViewActivity.this.productAdapter.clear();
                    ProductsViewActivity.this.productAdapter.addAll(list);
                    ProductsViewActivity.this.productAdapter.notifyDataSetChanged();
                } else {
                    List<ProductDBEntity> result = ProductsViewActivity.this.productDAO.getAllInGroup(item.getRemoteId());
                    List<ListViewItem> list = new LinkedList<>();

                    for (ProductDBEntity prod : result) list.add(prod.toListViewItem());

                    ProductsViewActivity.this.productAdapter.clear();
                    ProductsViewActivity.this.productAdapter.addAll(list);
                    ProductsViewActivity.this.productAdapter.notifyDataSetChanged();
                }
            }
        };
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
                String new_token = jo.getString(r.getString(R.string.token));

                this.token = Utilities.update_access_token(this.token, new_token);

                SyncTask task = new SyncTask(this.productAdapter, ref_token, this.token, productDAO);
                DownloadGroupsTask groupTask = new DownloadGroupsTask(this.groupAdapter,
                        ref_token, this.token, groupDAO);

                task.execute();
                groupTask.execute();
            } catch (JSONException ex) {
                Log.e("JSONException",
                        "Nie udało się sprarsować odpowiedzi z serwera :(");
            } catch (IOException ex) {
                Log.d("IOException",
                        "Brak połączenia z Internetem");
                Toast.makeText(this,
                    "Brak połączenia z Internetem",
                    Toast.LENGTH_SHORT).show();
            }
        }
        else
            Toast.makeText(this,
                    "Zaloguj się lub utwórz konto",
                    Toast.LENGTH_SHORT).show();
    }


    private void launch_reload_task() {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String ref_token = sp.getString(r.getString(R.string.refresh_token), "");
        Date ref_token_exp_date = Utilities.convert_to_date(sp.getString(r.getString(R.string.refresh_token_expiration_date), null));

        if (Utilities.is_ref_token_valid(ref_token_exp_date)) {
            try {
                RequestResult result = client.refresh(ref_token);
                JSONObject jo = result.getResponseBodyJSONObject();
                String new_token = jo.getString(r.getString(R.string.token));

                this.token = Utilities.update_access_token(this.token, new_token);

                ReloadTask task = new ReloadTask(this.productAdapter,
                        ref_token,
                        this.token,
                        productDAO);
                DownloadGroupsTask groupTask = new DownloadGroupsTask(this.groupAdapter,
                        ref_token,
                        this.token,
                        groupDAO);

                task.execute();
                groupTask.execute();
            } catch (JSONException ex) {
                Log.e("JSONException",
                        "Nie udało się sprarsować odpowiedzi z serwera :(");
            } catch (IOException ex) {
                Log.d("IOException",
                        "Brak połączenia z Internetem");
                Toast.makeText(this,
                        "Brak połączenia z internetem",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else
            Toast.makeText(this,
                    "Zaloguj się lub utwórz konto",
                    Toast.LENGTH_SHORT).show();
    }


    private void launch_download_groups_task() {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String ref_token = sp.getString(r.getString(R.string.refresh_token), "");
        Date ref_token_exp_date = Utilities.convert_to_date(sp.getString(r.getString(R.string.refresh_token_expiration_date), null));

        if (Utilities.is_ref_token_valid(ref_token_exp_date)) {
            try {
                RequestResult result = client.refresh(ref_token);
                JSONObject jo = result.getResponseBodyJSONObject();
                String new_token = jo.getString(r.getString(R.string.token));

                this.token = Utilities.update_access_token(this.token, new_token);

                DownloadGroupsTask groupTask = new DownloadGroupsTask(this.groupAdapter,
                        ref_token,
                        this.token,
                        groupDAO);

                groupTask.execute();
            } catch (JSONException ex) {
                Log.e("JSONException",
                        "Nie udało się sprarsować odpowiedzi z serwera :(");
            } catch (IOException ex) {
                Log.d("IOException",
                        "Brak połączenia z Internetem");
                Toast.makeText(this,
                        "Brak połączenia z internetem",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else
            Toast.makeText(this,
                    "Zaloguj się lub utwórz konto",
                    Toast.LENGTH_SHORT).show();
    }
}