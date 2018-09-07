package com.github.skomaromi.flack;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FlackApi {
    private static final String PROTO = "http";

    private static final String ENDPOINT_PING = "api/auth/ping/";
    private static final String ENDPOINT_LOGIN = "api/auth/login/";
    private static final String ENDPOINT_REGISTER = "api/auth/register/";
    private static final String ENDPOINT_ROOMS = "api/rooms/";

    private static final int HTTP_OK = 200;
    private static final String PING_EXPECTED_RESPONSE = "flack-pong";

    private String address;
    private String token;

    public FlackApi(String address) {
        this.address = address;
    }

    public FlackApi(String address, String token) {
        this.address = address;
        this.token = token;
    }

    public static boolean testConnection(String address) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                Constants.SERVER_PORT,
                ENDPOINT_PING
        );

        Request request = new Request.Builder()
                                  .url(url)
                                  .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        }
        catch (IOException e) {
            // server not accessible, but dump stack trace anyway for eventual
            // analysis
            e.printStackTrace();
            return false;
        }

        // make sure it's a Flack server
        String body;
        try {
            body = response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        String text;
        try {
            JSONObject responseJson = new JSONObject(body);
            text = responseJson.getString("text");
        }
        catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        return text.equals(PING_EXPECTED_RESPONSE);
    }

    public String login(String username, String password, Context context) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                Constants.SERVER_PORT,
                ENDPOINT_LOGIN
        );

        RequestBody requestBody = new FormBody.Builder()
                                          .add("username", username)
                                          .add("password", password)
                                          .build();


        Request request = new Request.Builder()
                                  .post(requestBody)
                                  .url(url)
                                  .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // check if HTTP 200 (OK)
        // response.code() will equal HTTP 403 (Forbidden) if provided
        //  credentials are invalid
        if (response.code() != HTTP_OK) {
            return null;
        }

        // retrieving token from response
        String responseBody;
        try {
            responseBody = response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String token, userName;
        int userId;
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            token = responseJson.getString("token");

            JSONObject userJson = responseJson.getJSONObject("user");
            userName = userJson.getString("name");
            userId = userJson.getInt("id");
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
        prefs.save(SharedPreferencesHelper.KEY_USERNAME, userName);
        prefs.save(SharedPreferencesHelper.KEY_USERID, userId);

        return token;
    }

    public String register(String username, String password, Context context) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                Constants.SERVER_PORT,
                ENDPOINT_REGISTER
        );

        RequestBody requestBody = new FormBody.Builder()
                                          .add("username", username)
                                          .add("password", password)
                                          .build();

        Request request = new Request.Builder()
                                  .post(requestBody)
                                  .url(url)
                                  .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // returns HTTP 400 (Bad Request) if provided username already exists
        if (response.code() != HTTP_OK) {
            return null;
        }

        String responseBody;
        try {
            responseBody = response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String token, userName;
        int userId;
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            token = responseJson.getString("token");

            JSONObject userJson = responseJson.getJSONObject("user");
            userName = userJson.getString("name");
            userId = userJson.getInt("id");
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
        prefs.save(SharedPreferencesHelper.KEY_USERNAME, userName);
        prefs.save(SharedPreferencesHelper.KEY_USERID, userId);

        return token;
    }

    public JSONArray getRoomsJson() {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                Constants.SERVER_PORT,
                ENDPOINT_ROOMS
        );

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("token", token);
        String queryUrl = urlBuilder.build().toString();

        Request request = new Request.Builder()
                                  .url(queryUrl)
                                  .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String responseBody;
        try {
            responseBody = response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        JSONArray roomsJson;
        try {
            roomsJson = new JSONArray(responseBody);
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return roomsJson;
    }
}
