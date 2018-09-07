package com.github.skomaromi.flack;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketsService extends Service {
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_AUTHTOKEN = "authtoken";

    private String address, token, usernameUnique;
    private FlackWebSocketListener listener;
    private SharedPreferencesHelper prefs;
    private boolean alreadyStarted;

    private class FlackWebSocketListener extends WebSocketListener {
        private int userId;

        public FlackWebSocketListener() {
            userId = prefs.getInt(SharedPreferencesHelper.KEY_USERID);
            Log.d(Constants.APP_NAME, String.format("FWSL: userId: %d", userId));
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);

        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(Constants.APP_NAME, "FlackWebSocketListener: okay, we got " +
                                              "something");
            Log.d(Constants.APP_NAME, String.format("FWSL: <msg>%s</msg>", text));


            // irrelevant room example
            // {
            //    "type":"notification",
            //    "attr":{
            //       "object":"room",
            //       "name":"ROOM TEST 3",
            //       "room_id":25,
            //       "sender":"admin",
            //       "sender_unique":"admin_mcs9",
            //       "participants":[
            //          1,
            //          10,
            //          9
            //       ]
            //    }
            // }

            // irrelevant message example
            // {
            //    "type":"notification",
            //    "attr":{
            //       "object":"message",
            //       "content":"dqwdwqqwdqwdwqdwdwqddw",
            //       "file":null,
            //       "room":25,
            //       "room_participants":[
            //          1,
            //          9,
            //          10
            //       ],
            //       "sender":"admin",
            //       "sender_unique":"admin_mcs9",
            //       "location":null,
            //       "message_id":112,
            //       "time":"2018-09-06 20:11:03.792252+00:00"
            //    }
            // }

            try {
                JSONObject textJson = new JSONObject(text);
                String textType = textJson.getString("type");

                //
                // response
                //
                if (textType.equals("response")) {
                    // TODO: do response stuff here
                }

                //
                // notification
                //
                else if (textType.equals("notification")) {
                    JSONObject attr = textJson.getJSONObject("attr");

                    String sender = attr.getString("sender");
                    int senderId = attr.getInt("sender_id");
                    String senderUnique = attr.getString("sender_unique");

                    String notificationObjectType = attr.getString("object");

                    if (notificationObjectType.equals("room")) {
                        // TODO: do room stuff here
                        JSONArray participantsJson = attr.getJSONArray("participants");

                        if (jsonArrayContains(participantsJson, userId)) {
                            if (senderId != userId) {
                                // TODO: make notification, add to db, list
                                Log.d(Constants.APP_NAME, "FWSL: new room | notif+db+list");
                            }
                            // same sender, different client
                            // we don't need "senderId == userId" because of
                            //  the previous if clause
                            else if (!senderUnique.equals(usernameUnique)) {
                                // TODO: add to db, list
                                Log.d(Constants.APP_NAME, "FWSL: new room | db+list");
                            }
                            else { Log.d(Constants.APP_NAME, "FWSL: new room | SAMECLIENT!"); }
                        }
                        else { Log.d(Constants.APP_NAME, "FWSL: new room | USERNOTINROOM!"); }
                    }
                    else if (notificationObjectType.equals("message")) {
                        // TODO: do message stuff here
                        JSONArray participantsJson = attr.getJSONArray("room_participants");

                        if (jsonArrayContains(participantsJson, userId)) {
                            if (senderId != userId) {
                                // TODO: make notification, add to db, list
                                Log.d(Constants.APP_NAME, "FWSL: new message | notif+db+list");
                            }
                            // same sender, different client
                            // we don't need "senderId == userId" because of
                            //  the previous if clause
                            else if (!senderUnique.equals(usernameUnique)) {
                                // TODO: add to db, list
                                Log.d(Constants.APP_NAME, "FWSL: new message | db+list");
                            }
                            else { Log.d(Constants.APP_NAME, "FWSL: new message | SAMECLIENT!"); }
                        }
                        else { Log.d(Constants.APP_NAME, "FWSL: new message | USERNOTINROOM!"); }
                    }
                }




            } catch (JSONException e) {
                e.printStackTrace();
            }

            // ignore irrelevant notifications
            // if response, store and UI
            // if notification:
            //   if from user's another client device, store and update UI
            //   if from other senders in rooms where participants include userId, store, UI and show notification
            // else ignore
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.APP_NAME, "WebSocketsService onCreate");

        alreadyStarted = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.APP_NAME, "WebSocketsService onStartCommand");

        if (!alreadyStarted) {
            handleIntentData(intent);

            prefs = new SharedPreferencesHelper(this);

            usernameUnique = String.format(
                    "%s_%s",

                    prefs.getString(SharedPreferencesHelper.KEY_USERNAME),
                    generateRandomString(4)
            );


            listener = new FlackWebSocketListener();

            if (!WebSocketSingleton.create()) {
                WebSocketSingleton.initialize(
                        address, token, usernameUnique, listener
                );
                WebSocketSingleton.create();
            }
        }

        alreadyStarted = true;
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void handleIntentData(Intent data) {

        if (data != null) {
            if (data.hasExtra(KEY_ADDRESS)) {
                address = data.getStringExtra(KEY_ADDRESS);
            }

            if (data.hasExtra(KEY_AUTHTOKEN)) {
                token = data.getStringExtra(KEY_AUTHTOKEN);
            }
        }
    }

    private String generateRandomString(int length) {
        Random r = new Random();
        StringBuilder str = new StringBuilder();

        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < length; i++)
            str.append(chars.charAt(r.nextInt(chars.length() - 1)));

        return str.toString();
    }

    private boolean jsonArrayContains(JSONArray array, Integer key) {
        for (int i = 0; i < array.length(); i++) {
            try {
                if (array.getInt(i) == key) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }
}
