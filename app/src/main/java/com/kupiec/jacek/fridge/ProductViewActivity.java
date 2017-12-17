package com.kupiec.jacek.fridge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kupiec.jacek.fridge.database.ProductDAO;
import com.kupiec.jacek.fridge.database.ProductDBEntitiy;
import com.kupiec.jacek.fridge.net.InvalidRefreshTokenException;
import com.kupiec.jacek.fridge.net.RequestResult;
import com.kupiec.jacek.fridge.net.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

public class ProductViewActivity extends AppCompatActivity {
    private String access_token = null;
    private Intent result_intent = new Intent();
    private boolean edited = false;
    private RestClient client = new RestClient();
    private ProductDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_view);

        Resources r = getResources();
        Intent intent = getIntent();
        this.access_token = intent.getStringExtra(r.getString(R.string.token));
        this.result_intent.putExtra(r.getString(R.string.position),
            intent.getIntExtra(r.getString(R.string.position),0));
        this.result_intent.putExtra(r.getString(R.string.should_reload), false);
        this.dao = new ProductDAO(getApplicationContext());

        ListViewItem item = (ListViewItem)intent.getSerializableExtra(r.getString(R.string.product));

        TextView nameTextView = findViewById(R.id.nameTextView);
        TextView storeNameTextView = findViewById(R.id.storeNameTextView);
        TextView priceTextView = findViewById(R.id.priceTextView);
        TextView amountTextView = findViewById(R.id.amountTextView);

        nameTextView.setText(item.getName());
        storeNameTextView.setText(item.getStoreName());
        priceTextView.setText(String.valueOf(item.getPrice()));
        amountTextView.setText(String.valueOf(item.getAmount()));
    }

    public void onRemoveProductButtonClick(View view) {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), null);
        Intent in_intent = getIntent();
        ListViewItem item = (ListViewItem)in_intent.getSerializableExtra(r.getString(R.string.product));

        try {
            ProductDBEntitiy product = this.dao.getProductById(item.getId());
            RequestResult result = this.client.delete_product(refresh_token,
                    this.access_token,
                    product.getRemoteId());

            update_access_token(result);

            switch(result.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    this.edited = false;
                    this.result_intent.putExtra(r.getString(R.string.product_status), ProductsViewActivity.PRODUCT_REMOVED);

                    this.dao.removeProdukt(product);
                    setResult(Activity.RESULT_OK, this.result_intent);
                    finish();
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
            this.dao.setAsRemoved(item.getId());
            this.edited = false;
            this.result_intent.putExtra(r.getString(R.string.product_status), ProductsViewActivity.PRODUCT_REMOVED);
            setResult(Activity.RESULT_OK, this.result_intent);
            finish();
        }
    }

    public void onDecreaseAmountButtonClick(View view) {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), null);
        Intent in_intent = getIntent();
        ListViewItem item = (ListViewItem)in_intent.getSerializableExtra(r.getString(R.string.product));
        EditText amountEditText = findViewById(R.id.amountEditText);
        TextView amountTextView = findViewById(R.id.amountTextView);

        if (amountEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Musisz podać o ile zmniejszyć", Toast.LENGTH_SHORT).show();
            return;
        }

        int delta = Integer.parseInt(amountEditText.getText().toString());

        try {
            ProductDBEntitiy product = this.dao.getProductById(item.getId());
            RequestResult result = this.client.decrease_amount(refresh_token,
                    this.access_token,
                    product.getRemoteId(),
                    delta);
            JSONObject jo;

            update_access_token(result);

            switch (result.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    this.edited = true;
                    jo = result.getResponseBodyJSONObject();
                    amountTextView.setText(convert_amount(jo));
                    this.dao.updateTotalAmount(product.getId(), jo.getInt(r.getString(R.string.product_amount)));
                    this.dao.changeSubtotalBy(product.getId(), -delta);
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
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udało się przetworzyć odpowiedzi z serwera");
        } catch (IOException ex) {
            this.edited = true;
            amountTextView.setText(String.valueOf(item.getAmount() - delta));
            this.dao.changeSubtotalBy(item.getId(), -delta);
            this.dao.changeTotalAmountBy(item.getId(), -delta);
            this.dao.setAsUpdated(item.getId());
        }
    }

    public void onIncreaseAmountButtonClick(View view) {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), null);
        Intent in_intent = getIntent();
        ListViewItem item = (ListViewItem)in_intent.getSerializableExtra(r.getString(R.string.product));
        EditText amountEditText = findViewById(R.id.amountEditText);
        TextView amountTextView = findViewById(R.id.amountTextView);
        ProductDBEntitiy product = this.dao.getProductById(item.getId());

        if (amountEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Musisz podać o ile zwiększyć", Toast.LENGTH_SHORT).show();
            return;
        }

        int delta = Integer.parseInt(amountEditText.getText().toString());

        try {
            RequestResult result = this.client.increase_amount(refresh_token,
                    this.access_token,
                    product.getRemoteId(),
                    delta);
            JSONObject jo;

            update_access_token(result);

            switch (result.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    this.edited = true;
                    jo = result.getResponseBodyJSONObject();
                    amountTextView.setText(convert_amount(jo));
                    this.dao.updateTotalAmount(product.getId(), jo.getInt(r.getString(R.string.product_amount)));
                    this.dao.changeSubtotalBy(product.getId(), delta);
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
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udalo sie przetworzy codpowiedzi z serwera");
        } catch (IOException ex) {
            this.edited = true;
            amountTextView.setText(String.valueOf(product.getTotal() + delta));
            this.dao.changeSubtotalBy(product.getId(), delta);
            this.dao.changeTotalAmountBy(product.getId(), delta);
            this.dao.setAsUpdated(product.getId());
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

    @NonNull
    private String convert_amount(JSONObject jo) throws JSONException {
        Resources r = getResources();

        return String.valueOf(jo.getInt(r.getString(R.string.product_amount)));
    }

}
