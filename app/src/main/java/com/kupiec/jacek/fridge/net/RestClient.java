package com.kupiec.jacek.fridge.net;

import android.net.Network;
import android.support.annotation.Nullable;
import android.support.v7.widget.ActivityChooserView;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RestClient {
    private final String SERVER_IP = "http://10.0.2.2:3000";
    private final String BAD_ACCESS_TOKEN = "Bad access token";

    public RequestResult add_product(String refresh_token, String token, ProductNet product) throws InvalidRefreshTokenException {
        JSONObject jo = new JSONObject();
        JSONObject p = new JSONObject();
        RequestResult result;

        try {
            p.put("name", product.getName());
            p.put("store_name", product.getStoreName());
            if (product.getPrice() >= 0) p.put("price", product.getPrice());
            if (product.getAmount() >= 0) p.put("amount", product.getAmount());
            jo.put("product", p);
            result = doPOST(SERVER_IP + "/products", token, jo.toString());

            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result; //udalo sie wyslac za pierwszym razem :)
            else {
                result =  doPOST(SERVER_IP + "/products", new_token, jo.toString());
                result.setRefreshedAccessToken(new_token);

                return result;
            }
        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie zamknac strumieni");
            return null;
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udalo sie utworzyc pliku JSON");
            return null;
        }
    }

    public RequestResult delete_product(String refresh_token, String token, int id) throws InvalidRefreshTokenException {
        try {
            String url = String.format("%s/products/%d", SERVER_IP, id);
            RequestResult result = doDELETE(url, token);
            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result;
            else {
                result =  doDELETE(url, new_token);
                result.setRefreshedAccessToken(new_token);

                return result;
            }
        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie Zamknac strumieni");
        }

        return null;
    }

    public RequestResult increase_amount(String refresh_token, String token, int id, int delta) throws InvalidRefreshTokenException {
        String url = String.format("%s/products/increase_amount/%d/%d", SERVER_IP, id, delta);

        try {
            RequestResult result = doPUT(url, token);
            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result;
            else {
                result =  doPUT(url, new_token);
                result.setRefreshedAccessToken(new_token);

                return result;
            }
        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie Zamknac strumieni");
        }

        return null;
    }

    public RequestResult decrease_amount(String refresh_token, String token, int id, int delta) throws InvalidRefreshTokenException {
        String url = String.format("%s/products/decrease_amount/%d/%d", SERVER_IP, id, delta);

        try {
            RequestResult result = doPUT(url, token);
            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result;
            else {
                result =  doPUT(url, new_token);
                result.setRefreshedAccessToken(new_token);

                return result;
            }

        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie Zamknac strumieni");
        }

        return null;
    }

    public RequestResult sync_subsum(String refresh_token, String token, int id, int subtotal) throws InvalidRefreshTokenException {
        String url = String.format("%s/products/sync_subsum/%d/%d", SERVER_IP, id, subtotal);

        try {
            RequestResult result = doPUT(url, token);
            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result;
            else {
                result =  doPUT(url, new_token);
                result.setRefreshedAccessToken(new_token);

                return result;
            }
        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie Zamknac strumieni");
        }

        return null;
    }

    public RequestResult get_product(String refresh_token, String token, int id) throws InvalidRefreshTokenException {
        String url = String.format("%s/products/%d", SERVER_IP, id);

        try {
            RequestResult result = doGET(url, token);
            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result;
            else {
                result =  doGET(url, new_token);
                result.setRefreshedAccessToken(new_token);

                return result;
            }
        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie Zamknac strumieni");
        }

        return null;
    }

    //To można wykorzystać jednocześnie do tworzenia urzytkownika i do logowania się
    public RequestResult send_user_data(String username, String password, String op) {
        JSONObject jo = new JSONObject();
        String url = String.format("%s/users/%s", SERVER_IP, op);

        try {
            jo.put("user", new JSONObject());
            jo.getJSONObject("user").put("name", username).put("password", password);

            return doPOST(url, "", jo.toString());
        } catch (IOException ex) {
            Log.e("IOException", "Nie udalo sie Zamknac strumieni");
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udalo sie utworzyc pliku JSON");
        }

        return null;
    }

    public RequestResult refresh(String refresh_token) {
        try {
            return doGET(SERVER_IP + "/users/refresh", refresh_token);
        } catch (IOException ex) {
            Log.e("IOException", "Błąd przy zamykaniu BufferedReader");
            return null;
        }
    }

    public RequestResult get_products(String refresh_token, String token) throws InvalidRefreshTokenException {
        try {
            RequestResult result = doGET(SERVER_IP + "/users/get_products", token);
            String new_token = refresh_access_token_if_necessary(result, refresh_token);

            if (new_token.isEmpty())
                return result;
            else {
                result =  doGET(SERVER_IP + "/users/get_products", new_token);
                result.setRefreshedAccessToken(new_token);

                return result;
            }
        } catch (IOException ex) {
            Log.e("IOException", "Błąd przy zamykaniu BufferedReader");
            return null;
        }
    }

    @Nullable
    private RequestResult doGET(String url, String token) throws IOException {
        NetworkTask task = new NetworkTask("GET", url, token, null);

        task.start();
        try {
            task.join();

            return task.getRequestResult();
        } catch (InterruptedException ex) {
            Log.e("InterruptedExc", "Wątek komunikacji z siecią został przerwany");
            return null;
        }

    }

    @Nullable
    private RequestResult doPOST(String url, String token, String content) throws IOException {
        NetworkTask task = new NetworkTask("POST", url, token, content);

        task.start();

        try {
            task.join();

            return task.getRequestResult();
        } catch (InterruptedException ex) {
            Log.e("InterruptedExc", "Wątek komunikacji z siecią został przerwany");
            return null;
        }
    }

    @Nullable
    private RequestResult doPUT(String url, String token) throws IOException {
        NetworkTask task = new NetworkTask("PUT", url, token, null);

        task.start();

        try {
            task.join();

            return task.getRequestResult();
        } catch (InterruptedException ex) {
            Log.e("InterruptedExc", "Wątek komunikacji z siecią został przerwany");
            return null;
        }
    }

    @Nullable
    private RequestResult doDELETE(String url, String token) throws IOException {
        NetworkTask task = new NetworkTask("DELETE", url, token, null);

        task.start();
        try {
            task.join();

            return task.getRequestResult();
        } catch (InterruptedException ex) {
            Log.e("InterruptedExc", "Wątek komunikacji z siecią został przerwany");
            return null;
        }
    }

    private boolean is_access_token_invalid (RequestResult result) {
        try {
            return result.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED &&
                result.getResponseBodyJSONObject().get("message").toString().compareTo(BAD_ACCESS_TOKEN) == 0;
        }catch (JSONException ex) {
            Log.e("JSONException", "Nie udało się przetworzyć odpowiedzi z serwera");
            return false;
        }
    }

    @Nullable
    private String get_access_token(RequestResult result) {
        try {
            return result.getResponseBodyJSONObject().get("token").toString();
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udało sie przetworzyć odpowiedzi od serwera");
        }

        return "";
    }

    private String refresh_access_token_if_necessary(RequestResult result, String refresh_token) throws InvalidRefreshTokenException {
        if (is_access_token_invalid(result)) {
            RequestResult res = this.refresh(refresh_token);

            if (res.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new InvalidRefreshTokenException();
            else
                return get_access_token(res);
        }

        return "";
    }

    private class NetworkTask extends Thread {
        private String method, url, token, content;
        private RequestResult result;

        public NetworkTask(String method, String url, String token, @Nullable String content) {
            this.method = method;
            this.url = url;
            this.token = token;
            this.content = content;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            DataOutputStream data = null;

            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod(this.method);
                con.setRequestProperty("Authorization", "Token token=" + this.token);
                con.setDoInput(true);

                if (content != null) {
                    byte[] array = this.content.getBytes();

                    con.setRequestProperty("Content-Type", "application/json");
                    con.setRequestProperty("Content-Length", Integer.toString(array.length));
                    con.setDoOutput(true);
                    data = new DataOutputStream(con.getOutputStream());
                    data.write(array);
                    data.close();
                }

                int responseCode = con.getResponseCode();

                if (200 <= responseCode && responseCode < 300)
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                else
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));

                StringBuilder builder = new StringBuilder();
                String input_line;

                while ((input_line = br.readLine()) != null) builder.append(input_line);
                br.close();

                this.result = new RequestResult(builder.toString(), responseCode);
            } catch (MalformedURLException ex) {
                Log.e("MalformedURLException", "Niepoprawny adres URl");
            } catch (ProtocolException ex) {
                Log.e("ProtocolException", "Źle ustawiona metoda HTTP");
            } catch (IOException ex) {
                Log.e("IOException", "Nie ma połączenia z serwisem, nie udało się pobrać danych");
            } finally {
                try {
                    if (br != null) br.close();
                    if (data != null) data.close();
                } catch (IOException ex) {
                    Log.e("IOException", "Nie udalo sie zamknąć strumieni!");
                }
            }
        }

        RequestResult getRequestResult() {
            return result;
        }
    }
}
