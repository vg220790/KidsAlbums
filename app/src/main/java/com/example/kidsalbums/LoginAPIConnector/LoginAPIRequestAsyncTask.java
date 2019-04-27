package com.example.kidsalbums.LoginAPIConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginAPIRequestAsyncTask extends AsyncTask<String , Void ,HashMap<String, String>> {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    String server_response;
    JSONObject json_request_body;
    String LOGIN_URL = "http://51.140.224.39:13456/login-api";

    @Override
    protected HashMap<String, String> doInBackground(String... strings) {
        //ArrayList<String> userInfo = new ArrayList<String>();

        String username = strings[0];
        String password = strings[1];
        String json_request_body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        try {

            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(JSON, json_request_body);
            Request request = new Request.Builder()
                    .url(LOGIN_URL)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String res = response.body().string();
                JSONObject json = new JSONObject(res);
                HashMap<String,String> userInfo = getUserInfoFromJsonResponse(json);
                return userInfo;
            }
        } catch (IOException ie) {
            ie.printStackTrace();
        }catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(HashMap<String, String> s) {
        super.onPostExecute(s);

    }

    protected HashMap<String, String> getUserInfoFromJsonResponse(JSONObject json) throws JSONException {
        HashMap<String, String> userInfo = new HashMap<String, String>();
        JSONObject user_info = json.getJSONObject("userInfo");
        JSONObject child_info = json.getJSONArray("childInfo").getJSONObject(0);
        JSONObject kindergarten_info = json.getJSONArray("kindergartenInfo").getJSONObject(0);


        userInfo.put("id",user_info.getString("_id"));
        userInfo.put("email",user_info.getString("username"));
        userInfo.put("name",user_info.getString("name"));
        userInfo.put("phone",user_info.getString("nationalFormat"));

        userInfo.put("childId",child_info.getString("_id"));
        userInfo.put("childName",child_info.getString("name"));
        userInfo.put("childBirthday",child_info.getString("bDay"));
        userInfo.put("childTag",child_info.getString("tag"));

        userInfo.put("kindergartenId",kindergarten_info.getString("_id"));
        userInfo.put("kindergartenName",kindergarten_info.getString("name"));
        userInfo.put("kindergartenEmail",kindergarten_info.getString("email"));
        userInfo.put("kindergartenAddress",kindergarten_info.getString("address"));
        ////
        userInfo.put("kindergartenLatitude",kindergarten_info.getString("latitude"));
        userInfo.put("kindergartenLongitude",kindergarten_info.getString("longtitude"));

        return userInfo;
    }

}