package com.kupiec.jacek.fridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.kupiec.jacek.fridge.database.ProductDAO;
import com.kupiec.jacek.fridge.net.RequestResult;
import com.kupiec.jacek.fridge.net.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Może dojść do sytuacji, w której refresh token wygaśnie i trzeba będzie zalogować się na soje
 * konto jeszcze raz. W takim przypadku, jeżeli użytkownik loguje się na inne konto to usuwam dane
 * poprzednika. Jeżeli na to samo to bazę muszę zostawić, a przynajmniej powinienem. Dlatego serwer
 * będzie wysyłał informację czy użytkownik ponownie zalogował sie na to samo konto czy też nie.
 * Jeżeli konto będzie to samo to nie trzeba przeładowywać widoku
 */

public class LogInActivity extends AppCompatActivity {
    private RestClient client = new RestClient();
    private ProductDAO dao;
    private boolean drop_out_changes = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        this.dao = new ProductDAO(getApplicationContext());
    }

    public void onClickLogInButton(View view) {
        Resources r = getResources();
        EditText username_edittext = findViewById(R.id.usernameEditText);
        EditText password_edittext = findViewById(R.id.passwordEditText);
        String username = username_edittext.getText().toString();
        String password = password_edittext.getText().toString();
        SharedPreferences sp = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE);
        String old_refresh_token = sp.getString(r.getString(R.string.refresh_token), "");

        if (warn_about_data_loss()) return;

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Podaj login i hasło", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            RequestResult result = this.client.send_user_data(username,
                    password,
                    old_refresh_token,
                    "log_in");

            if (result.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Intent intent = new Intent();
                JSONObject jo = result.getResponseBodyJSONObject();
                String refresh_token = jo.getString(r.getString(R.string.refresh_token));
                String refresh_token_expiration_date = jo.getString(
                        r.getString(R.string.refresh_token_expiration_date));
                boolean should_reload = jo.getBoolean(r.getString(R.string.should_reload));

                result = this.client.refresh(refresh_token);
                jo = result.getResponseBodyJSONObject();

                String token = jo.getString(r.getString(R.string.token));

                intent.putExtra(r.getString(R.string.token), token);
                intent.putExtra(r.getString(R.string.should_reload), should_reload);
                save_refresh_token(refresh_token, refresh_token_expiration_date);
                setResult(Activity.RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this,
                        "Błędne dane logowania",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException ex) {
            Toast.makeText(this,
                    "Brak połączenia z internetem",
                    Toast.LENGTH_SHORT).show();
        } catch (JSONException ex) {
            Log.e("JSONException",
                    "Nie można przetworzyć danych uwirzytelniających nadesłanych przez serwer");
            Toast.makeText(this,
                    "Błędna odpowiedź serwera",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickCreateUserButton(View view) {
        Resources r = getResources();
        EditText username_edittext = findViewById(R.id.usernameEditText);
        EditText password_edittext = findViewById(R.id.passwordEditText);
        String username = username_edittext.getText().toString();
        String password = password_edittext.getText().toString();

        if (warn_about_data_loss()) return;
        
        try {
            RequestResult result = this.client.send_user_data(username,
                    password,
                    null,
                    "");

            if (result.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                Intent intent = new Intent();
                JSONObject jo = result.getResponseBodyJSONObject();
                String refresh_token = jo.getString(r.getString(R.string.refresh_token));
                String refresh_token_exppiration_date = jo.getString(
                        r.getString(R.string.refresh_token_expiration_date));
                String token = jo.getString(r.getString(R.string.token));

                intent.putExtra(r.getString(R.string.token), token);
                intent.putExtra(r.getString(R.string.should_reload), true);
                save_refresh_token(refresh_token, refresh_token_exppiration_date);
                setResult(Activity.RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(this,
                        "Nie udało się utworzyć konta",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException ex) {
            Toast.makeText(this,
                    "Brak połączenia z internetem",
                    Toast.LENGTH_SHORT).show();
        } catch (JSONException ex) {
            Log.e("JSONException",
                    "Nie można przetworzyć danych uwirzytelniających nadesłanych przez serwer");
            Toast.makeText(this,
                    "Błędna odpowiedź serwera",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    private void save_refresh_token(String refresh_token, String refresh_token_exp_date) {
        Resources r = getResources();
        SharedPreferences.Editor editor = getSharedPreferences(r.getString(R.string.prefrences_token), MODE_PRIVATE).edit();

        editor.putString(r.getString(R.string.refresh_token), refresh_token);
        editor.putString(r.getString(R.string.refresh_token_expiration_date), refresh_token_exp_date);
        editor.commit();
    }

    private AlertDialog create_alert_dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int witch) {
                switch(witch) {
                case DialogInterface.BUTTON_POSITIVE:
                    LogInActivity.this.drop_out_changes = true;
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    LogInActivity.this.drop_out_changes = false;
                    break;
                }
            }
        };

        builder.setMessage(R.string.not_saved_data_message);
        builder.setPositiveButton("Tak", listener);
        builder.setNegativeButton("Nie", listener);

        return builder.create();
    }

    private boolean warn_about_data_loss() {
        if (dao.isThereAnyChanges()) {
            AlertDialog dialog = create_alert_dialog();

            dialog.show();

            if (!this.drop_out_changes) {
                Toast.makeText(this, "Zsynchronizuj przed zmianą konta", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        return false;
    }
}
