package com.kupiec.jacek.fridge.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.kupiec.jacek.fridge.SpinnerItem;
import com.kupiec.jacek.fridge.database.GroupDAO;
import com.kupiec.jacek.fridge.database.GroupDBEntity;
import com.kupiec.jacek.fridge.net.InvalidRefreshTokenException;
import com.kupiec.jacek.fridge.net.RequestResult;
import com.kupiec.jacek.fridge.net.RestClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jacek on 29.01.18.
 */

public class DownloadGroupsTask extends AsyncTask<Void, Void, List<SpinnerItem>> {
    private ArrayAdapter<SpinnerItem> groupAdapter;
    private String refresh_token, token;
    private GroupDAO groupDAO;

    public DownloadGroupsTask(ArrayAdapter<SpinnerItem> groupAdapter, String refresh_token, String token, GroupDAO groupDAO) {
        this.groupAdapter = groupAdapter;
        this.refresh_token = refresh_token;
        this.token = token;
        this.groupDAO = groupDAO;
    }


    @Override
    protected List<SpinnerItem> doInBackground(Void... args) {
        RestClient client = new RestClient();
        List<SpinnerItem> list = new LinkedList<>();

        try {
            RequestResult result = client.get_groups(this.refresh_token, this.token);

            if (result.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONArray ja = result.getResponseBodyJSONObject().getJSONArray("groups");

                groupDAO.truncateGroupsTable();

                for (int i = 0; i < ja.length(); i++) {
                    long id = ja.getJSONObject(i).getInt("id");
                    String name = ja.getJSONObject(i).getString("name");

                    list.add(new SpinnerItem(id, name));
                }
            } else {
                return load_from_db(groupDAO);
            }
        } catch (InvalidRefreshTokenException ex) {
            Log.e("InvalidRefershToken",
            "Nie można wykonać operacji ponieważ refresh token, który został podany nie działa");
            return load_from_db(groupDAO);
        } catch (IOException ex) {
            Log.e("IOException", "Brak połączenia z internetem");
            return load_from_db(groupDAO);
        } catch (JSONException ex) {
            Log.e("JSONException", "Nie udało się przetwrzoyć odpowiedzi z serwera");
            return load_from_db(groupDAO);
        }

        return list;
    }

    protected void onPostExecute(List<SpinnerItem> result) {
        if (result == null) {
            Log.d("Synchronization FAILED", "Nie udało się poprawnie wykonać synchronizacji");
        } else {
            this.groupAdapter.clear();
            this.groupAdapter.addAll(result);
            this.groupAdapter.notifyDataSetChanged();
            Log.d("Synchronization SUCCEED", "Synchronizację przeprowadzono poprawnie :)");
        }
    }

    public static List<SpinnerItem> load_from_db(GroupDAO dao) {
        List<SpinnerItem> list = new LinkedList<>();

        for (GroupDBEntity group: dao.getAllGroups())
            list.add(new SpinnerItem(group.getRemoteId(), group.getName()));

        return list;
    }
}
