package com.kupiec.jacek.fridge;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import com.kupiec.jacek.fridge.database.ProductDAO;
import com.kupiec.jacek.fridge.database.ProductDBEntitiy;
import com.kupiec.jacek.fridge.net.InvalidRefreshTokenException;
import com.kupiec.jacek.fridge.net.ProductNet;
import com.kupiec.jacek.fridge.net.RequestResult;
import com.kupiec.jacek.fridge.net.RestClient;

import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.UUID;

public class CreateProductActivity extends AppCompatActivity {
    private RestClient client = new RestClient();
    private Intent result_intent = new Intent();
    private String access_token;
    private ProductDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_product);

        Resources r = getResources();
        Intent intent = getIntent();
        this.access_token = intent.getStringExtra(r.getString(R.string.token));
        this.result_intent.putExtra(r.getString(R.string.should_reload), false);
        this.dao = new ProductDAO(this.getApplicationContext());
    }

    //TODO: można wpisywać dużo produktów, nie trzeba wychodzić z aktywności po jednym dodaniu
    public void onClickCreateProductButton(View view) {
        Resources r = getResources();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String refresh_token = sp.getString(r.getString(R.string.refresh_token), null);

        EditText nameEditText = findViewById(R.id.nameOfProductEditText);
        EditText storeNameEditText = findViewById(R.id.nameOfStoreEditText);
        EditText priceEditText = findViewById(R.id.priceofProductEditText);
        EditText amountEditText = findViewById(R.id.amountOfProductEditText);
        EditText brandEditText = findViewById(R.id.brandOfProductEditText);

        String name = nameEditText.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Musisz podać nazwę produktu", Toast.LENGTH_SHORT).show();
            return;
        }

        String store_name = storeNameEditText.getText().toString();
        String brand = brandEditText.getText().toString();
        double price = round_price(priceEditText.getText().toString());
        int amount = get_amount(amountEditText.getText().toString());
        ProductNet product = new ProductNet(name,
                store_name,
                price,
                amount,
                UUID.randomUUID().toString(),
                brand);

        try {
            RequestResult result = this.client.add_product(refresh_token, this.access_token, product);

            if (!result.getRefreshedAccessToken().isEmpty()) {
                this.access_token = result.getRefreshedAccessToken();
                this.result_intent.putExtra(r.getString(R.string.token), this.access_token);
            }

            if (result.getResponseCode() == HttpURLConnection.HTTP_OK) {
                int remote_id = result.getResponseBodyJSONObject().getInt("id");
                long db_id = dao.addProduct(new ProductDBEntitiy(product.getName(),
                        product.getStoreName(),
                        product.getPrice(),
                        product.getAmount(),
                        product.getAmount(), //Skoro utowrzyłem cały produkt to mam u siebie jedyną deltę
                        0, 0, 0,
                        remote_id,
                        product.getGUID(),
                        product.getBrand()));

                this.result_intent.putExtra(r.getString(R.string.product), product.toListViewItem(db_id));
                setResult(Activity.RESULT_OK, this.result_intent);
                finish();
            } else if (result.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST){
                Toast.makeText(this,
                        "Nieprawidłowe dane dla utworzenia produktu",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (InvalidRefreshTokenException ex) {
            Intent intent = new Intent(this, LogInActivity.class);

            Log.d("InvalidRefreshTokenExc",
                    "Użytkownik musi się ponownie zalogować");
            Toast.makeText(this,
                    "Zaloguj się",
                    Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, ProductsViewActivity.LOG_IN_ACTIVITY);
        } catch (IOException ex) {
            long db_id = dao.addProduct(new ProductDBEntitiy(product.getName(),
                    product.getStoreName(),
                    product.getPrice(),
                    product.getAmount(),
                    product.getAmount(),
                    1, 0, 0,
                    -1,
                    product.getGUID(),
                    product.getBrand()));

            this.result_intent.putExtra(r.getString(R.string.product), product.toListViewItem(db_id));
            setResult(Activity.RESULT_OK, this.result_intent);
            finish();
        } catch (JSONException ex) {
            Log.e("JSONException",
                    "Nie udalo sie przetworzyc odpowiedzi od serwera");
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED, this.result_intent);
        super.onBackPressed();
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

    private double round_price(String price) {
        if (price != null && !price.isEmpty())
            return Math.floor(Double.parseDouble(price)*100.0) / 100;
        else
            return 0;
    }

    private int get_amount(String amount) {
        if (amount != null && !amount.isEmpty())
            return Integer.parseInt(amount);
        else
            return 0;
    }
}
