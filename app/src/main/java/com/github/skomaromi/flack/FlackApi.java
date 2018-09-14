package com.github.skomaromi.flack;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FlackApi {
    private static final String ENDPOINT_PING = "api/auth/ping/";
    private static final String ENDPOINT_LOGIN = "api/auth/login/";
    private static final String ENDPOINT_REGISTER = "api/auth/register/";
    private static final String ENDPOINT_ROOMS = "api/rooms/";
    private static final String ENDPOINT_MESSAGES = "api/messages/";
    private static final String ENDPOINT_FILES = "api/files/";
    private static final String ENDPOINT_FILEUPLOAD = "api/files/upload/";
    private static final String ENDPOINT_USERS = "api/auth/users/";

    private static final int HTTP_OK = 200;
    private static final String PING_EXPECTED_RESPONSE = "flack-pong";

    private String address;
    private String token;

    public FlackApi(Context context) {
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
        address = prefs.getString(SharedPreferencesHelper.KEY_SERVERADDR);
        token = prefs.getString(SharedPreferencesHelper.KEY_AUTHTOKEN);
    }

    public FlackApi(String address) {
        this.address = address;
    }

    public static boolean testConnection(String address) {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s",

                Constants.SERVER_PROTO,
                address,
                Constants.SERVER_PORT,
                ENDPOINT_PING
        );

        Request request = new Request.Builder()
                                  .url(url)
                                  .build();

        Response response;
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

                Constants.SERVER_PROTO,
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

        Response response;
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

                Constants.SERVER_PROTO,
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

        Response response;
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

    public ArrayList<File> getFiles() {
        ArrayList<File> files = new ArrayList<>();

        OkHttpClient client = new OkHttpClient();

        HttpUrl queryUrl = new HttpUrl.Builder()
                                   .scheme(Constants.SERVER_PROTO)
                                   .host(address)
                                   .port(Constants.SERVER_PORT)
                                   .addPathSegment(ENDPOINT_FILES)
                                   .addQueryParameter("token", token)
                                   .build();

        Request request = new Request.Builder()
                                  .get()
                                  .url(queryUrl)
                                  .build();

        Response response;
        String responseBody;
        try {
            response = client.newCall(request).execute();
            responseBody = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return files;
        }

        try {
            JSONArray fileJsons = new JSONArray(responseBody);

            for (int i = 0; i < fileJsons.length(); i++) {
                JSONObject fileJson = fileJsons.getJSONObject(i);

                int serverId = fileJson.getInt("id");
                String hash = fileJson.getString("hash");
                String name = fileJson.getString("name");
                String sizeStr = fileJson.getString("size");

                File file = new File(
                        serverId,
                        hash,
                        name,
                        sizeStr
                );
                files.add(file);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            return files;
        }

        return files;
    }

    public int uploadFile(String filePath, Context context) {
        // returns file ID on server
        int fileId;

        java.io.File file = new java.io.File(filePath);

        OkHttpClient client = new OkHttpClient();

        HttpUrl queryUrl = new HttpUrl.Builder()
                                   .scheme(Constants.SERVER_PROTO)
                                   .host(address)
                                   .port(Constants.SERVER_PORT)
                                   .addPathSegment(ENDPOINT_FILEUPLOAD)
                                   .build();

        String mimeType = URLConnection.guessContentTypeFromName(filePath);
        MediaType type;
        if (mimeType == null) {
            type = MediaType.get("application/octet-stream");
        }
        else {
            type = MediaType.get(mimeType);
        }
        RequestBody fileRequestBody = RequestBody.create(type, file);

        String fileName = file.getName();

        RequestBody requestBody = new MultipartBody.Builder()
                                          .setType(MultipartBody.FORM)
                                          .addFormDataPart("file", fileName, fileRequestBody)
                                          .addFormDataPart("token", token)
                                          .build();

        Request request = new Request.Builder()
                                  .put(requestBody)
                                  .url(queryUrl)
                                  .build();

        Response response;
        String responseBody;
        try {
            response = client.newCall(request).execute();
            responseBody = response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        String fileHash, fileSizeStr;
        try {
            // example response:
            // {
            //   "message":"success",
            //   "file":{
            //      "name":"<fileName>",
            //      "hash":"<fileHash>",
            //      "size":"999.9 kB",
            //      "url":"https://ipfs.io/ipfs/<fileHash>/<fileName>",
            //      "id":56
            //   }
            // }
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONObject fileJson = jsonResponse.getJSONObject("file");

            fileId = fileJson.getInt("id");
            fileHash = fileJson.getString("hash");
            fileName = fileJson.getString("name");
            fileSizeStr = fileJson.getString("size");
        }
        catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }

        if (fileId != -1) {
            SqlHelper sqlHelper = new SqlHelper(context);
            sqlHelper.addFile(
                    fileId,
                    fileHash,
                    fileName,
                    fileSizeStr
            );
        }

        return fileId;
    }

    public ArrayList<User> getUsers() {
        ArrayList<User> users = new ArrayList<>();

        OkHttpClient client = new OkHttpClient();

        HttpUrl queryUrl = new HttpUrl.Builder()
                                   .scheme(Constants.SERVER_PROTO)
                                   .host(address)
                                   .port(Constants.SERVER_PORT)
                                   .addPathSegment(ENDPOINT_USERS)
                                   .addQueryParameter("token", token)
                                   .build();

        Request request = new Request.Builder()
                                  .get()
                                  .url(queryUrl)
                                  .build();

        Response response;
        String responseBody;
        try {
            response = client.newCall(request).execute();
            responseBody = response.body().string();
        }
        catch (IOException e) {
            e.printStackTrace();
            return users;
        }

        try {
            // example response:
            // [
            //    {
            //       "id":2,
            //       "username":"anotheruser"
            //    },
            //    {
            //       "id":3,
            //       "username":"exampleuser"
            //    }
            // ]
            JSONArray userJsons = new JSONArray(responseBody);

            for (int i = 0; i < userJsons.length(); i++) {
                JSONObject userJson = userJsons.getJSONObject(i);

                int serverId = userJson.getInt("id");
                String username = userJson.getString("username");

                User user = new User(serverId, username);
                users.add(user);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            return users;
        }

        return users;
    }
}
