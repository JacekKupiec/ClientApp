package com.kupiec.jacek.fridge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.kupiec.jacek.fridge.database.GroupDAO;
import com.kupiec.jacek.fridge.database.GroupDBEntity;
import com.kupiec.jacek.fridge.net.InvalidRefreshTokenException;
import com.kupiec.jacek.fridge.net.RequestResult;
import com.kupiec.jacek.fridge.net.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

public class GroupsActivity extends AppCompatActivity {
    private GroupDAO groupDAO;
    private String access_token = null;
    private Intent result_intent = new Intent();
    private boolean edited = false;
    private RestClient client = new RestClient();
    private ArrayAdapter<SpinnerItem> groupsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        Resources r = getResources();
        Intent intent = getIntent();
        Spinner groupToRemoveSpinner = findViewById(R.id.groupToRemoveSpinner);

        this.groupsAdapter = new ArrayAdapter<SpinnerItem>(this, R.layout.list_item);
        this.access_token = intent.getStringExtra(r.getString(R.string.token));
        this.result_intent.putExtra(r.getString(R.string.position), intent.getIntExtra(r.getString(R.string.position),0));
        this.result_intent.putExtra(r.getString(R.string.should_reload), false);
        this.groupDAO = new GroupDAO(getApplicationContext());

        this.groupsAdapter.addAll(Utilities.load_groups_from_db(this.groupDAO));
        groupToRemoveSpinner.setAdapter(this.groupsAdapter);
    }


    public void onRemoveGroupButtonClick(View view) {
        Spinner groupToRemoveSpinner = findViewById(R.id.groupToRemoveSpinner);
        SpinnerItem item = (SpinnerItem) groupToRemoveSpinner.getSelectedItem();
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), null);

        if (item == null) {
            Toast.makeText(this, "Nie wybrano grupy do usunięcia", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            RequestResult result = this.client.remove_group(refresh_token, this.access_token, item.getRemoteId());

            update_access_token(result);

            switch (result.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    this.edited = true;
                    this.groupsAdapter.remove(item);
                    this.groupsAdapter.notifyDataSetChanged();
                    this.groupDAO.removeGroupByRemoteId(item.getRemoteId());
                    Toast.makeText(this, "Grupę usunięto poprawnie", Toast.LENGTH_SHORT).show();
                    break;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    Log.e("BAD REQUEST", "Podano niewłaściwe parametry do usuniecia produktu");
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.e("UNAUTHORIZED", "Próba usunięcia produktu, który nie należał do użytkownika");
                    break;
            }
        } catch (InvalidRefreshTokenException ex) {
            Intent intent = new Intent(this, LogInActivity.class);
            startActivityForResult(intent, ProductsViewActivity.LOG_IN_ACTIVITY);
        } catch (IOException ex) {
            Toast.makeText(this, "Usuwanie grup tylko online", Toast.LENGTH_SHORT).show();
        }
    }


    public void onCreateGroupButtonClick(View view) {
        EditText nameOfGroupEditText = findViewById(R.id.nameOfGroupEditText);
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), null);
        String name = nameOfGroupEditText.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Nazwij grupę", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            RequestResult result = this.client.add_group(refresh_token, this.access_token, name);

            update_access_token(result);

            switch (result.getResponseCode()) {
                case HttpURLConnection.HTTP_CREATED:
                    JSONObject jo = result.getResponseBodyJSONObject();
                    SpinnerItem item = new SpinnerItem(jo.getInt("id"), name);

                    this.edited = true;
                    this.groupsAdapter.add(item);
                    this.groupsAdapter.notifyDataSetChanged();
                    this.groupDAO.addGroup(new GroupDBEntity(name, item.getRemoteId()));
                    Toast.makeText(this, "Dodano grupę", Toast.LENGTH_SHORT).show();
                    break;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    Log.e("BAD REQUEST", "Podano niewłaściwe parametry do usuniecia produktu");
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.e("UNAUTHORIZED", "Próba usunięcia produktu, który nie należał do użytkownika");
                    break;
            }
        } catch (InvalidRefreshTokenException ex) {
            Intent intent = new Intent(this, LogInActivity.class);
            startActivityForResult(intent, ProductsViewActivity.LOG_IN_ACTIVITY);
        } catch (IOException ex) {
            Toast.makeText(this, "Usuwanie grup tylko online", Toast.LENGTH_SHORT).show();
        } catch (JSONException ex) {
            Log.d("JSONException", "Niepoprawne parsowanie odpowiedzi z serwera");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ProductsViewActivity.LOG_IN_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                Resources r = getResources();
                String new_token = data.getStringExtra(r.getString(R.string.token));
                Boolean should_reload = data.getBooleanExtra(r.getString(R.string.should_reload),
                        false);

                this.access_token = new_token;
                this.result_intent.putExtra(r.getString(R.string.token), new_token);
                this.result_intent.putExtra(r.getString(R.string.should_reload), should_reload);
            }
        }
    }


    @Override
    public void onBackPressed() {
        Resources r = getResources();

        if (this.edited) {
            this.result_intent.putExtra(r.getString(R.string.product_status), ProductsViewActivity.PRODUCT_MODIFIED);
            setResult(ProductsViewActivity.RESULT_OK, this.result_intent);
        }
        else setResult(Activity.RESULT_CANCELED, this.result_intent);

        super.onBackPressed();
    }


    private void update_access_token(RequestResult result) {
        Resources r = getResources();

        if (!result.getRefreshedAccessToken().isEmpty()) {
            this.access_token = result.getRefreshedAccessToken();
            this.result_intent.putExtra(r.getString(R.string.token), this.access_token);
        }
    }
}
