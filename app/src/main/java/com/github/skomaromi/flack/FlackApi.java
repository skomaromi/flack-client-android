package com.github.skomaromi.flack;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class FlackApi {
    private static final String PROTO = "http";
    private static final int SERVER_PORT = 8000;

    private static final String ENDPOINT_PING = "api/auth/ping/";
    private static final String ENDPOINT_LOGIN = "api/auth/login/";
    private static final String ENDPOINT_REGISTER = "api/auth/register/";

    private static final int HTTP_OK = 200;
    private static final String PING_EXPECTED_RESPONSE = "flack-pong";

    private String address;

    public FlackApi(String address) {
        this.address = address;
    }

    public static boolean testConnection(String address) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                SERVER_PORT,
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
            // an expected exception type - this one neatly tells us that
            // the server does *not* exist
            if (e.getClass()
                        .getSimpleName().equals("UnknownHostException")) {
                return false;
            }

            // all other exceptions are completely unexpected
            e.printStackTrace();
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

    public String login(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                SERVER_PORT,
                ENDPOINT_LOGIN
        );

        RequestBody requestBody = new FormEncodingBuilder()
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

        String token;
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            token = responseJson.getString("token");
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return token;
    }

    public String register(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                PROTO,
                address,
                SERVER_PORT,
                ENDPOINT_REGISTER
        );

        RequestBody requestBody = new FormEncodingBuilder()
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

        String token;
        try {
            JSONObject responseJson = new JSONObject(responseBody);
            token = responseJson.getString("token");
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return token;
    }
}
