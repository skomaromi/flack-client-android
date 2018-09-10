package com.github.skomaromi.flack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketService extends Service {
    public static final int NOT_ON_MESSAGE_ACTIVITY = -1;
    public static final int TYPE_ROOM = 1;
    public static final int TYPE_MESSAGE = 2;
    public static final int TYPE_MESSAGELITE = 3;

    public static final String KEY_OBJECTTYPE = "type";

    public static final String KEY_ROOM_SERVERID = "server_id";
    public static final String KEY_ROOM_NAME = "name";
    public static final String KEY_ROOM_TIMECREATED = "time_created";

    public static final String KEY_MESSAGE_SENDER = "sender";
    public static final String KEY_MESSAGE_CONTENT = "content";
    public static final String KEY_MESSAGE_TIMECREATED = "time_created";
    public static final String KEY_MESSAGE_LOCATION_ISNULL = "location_null";
    public static final String KEY_MESSAGE_LOCATION_LATITUDE = "latitude";
    public static final String KEY_MESSAGE_LOCATION_LONGITUDE = "longitude";
    public static final String KEY_MESSAGE_FILE_ISNULL = "file_null";
    public static final String KEY_MESSAGE_FILE_HASH = "hash";
    public static final String KEY_MESSAGE_FILE_NAME = "name";

    public static final String KEY_MESSAGELITE_ROOMID = "room_id";
    public static final String KEY_MESSAGELITE_TEXT = "text";
    public static final String KEY_MESSAGELITE_TIMECREATED = "time_created";

    private final IBinder mBinder = new LocalBinder();
    private SharedPreferencesHelper prefs;
    private FlackWebSocketListener listener;
    private SqlHelper sqlHelper;
    private String usernameUnique;
    private boolean alreadyStarted, bound;
    private int currentRoom;

    private class FlackWebSocketListener extends WebSocketListener {
        private int userId;

        public FlackWebSocketListener() {
            userId = prefs.getInt(SharedPreferencesHelper.KEY_USERID);
            Log.d(Constants.APP_NAME, String.format("FWSL: userId: %d", userId));
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            Log.d(Constants.APP_NAME, "FWSL: websocket open!");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            Log.d(Constants.APP_NAME, "FWSL: websocket failure");
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            Log.d(Constants.APP_NAME, "FWSL: websocket disconnected!");
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
            //       "id":25,
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
                    // if response, store and UI
                }

                //
                // notification
                //
                else if (textType.equals("notification")) {
                    JSONObject attr = textJson.getJSONObject("attr");

                    String sender = attr.getString("sender");
                    int senderId = attr.getInt("sender_id");
                    String senderUnique = attr.getString("sender_unique");

                    boolean onRoomList = bound && currentRoom == NOT_ON_MESSAGE_ACTIVITY;

                    String notificationObjectType = attr.getString("object");

                    if (notificationObjectType.equals("room")) {
                        // DONE: do room stuff here
                        JSONArray participantsJson = attr.getJSONArray("participants");

                        // room db data
                        int id = attr.getInt("id");
                        String name = attr.getString("name");
                        long created = attr.getLong("time");

                        if (jsonArrayContains(participantsJson, userId)) {
                            boolean differentUser = senderId != userId;

                            //boolean sameUserAnotherClient = !differentUser && !senderUnique.equals(usernameUnique);
                            boolean anyOtherDeviceThanThis = !senderUnique.equals(usernameUnique);

                            //boolean shouldSyncAddDb = differentUser || sameUserAnotherClient;

                            boolean shouldSendNotification = differentUser && !onRoomList;

                            // boolean shouldSendBroadcast = differentUser || sameUserAnotherClient;

                            if (anyOtherDeviceThanThis) {
                                updateRoomSyncData(id, created);
                                sqlHelper.addRoom(id, name, created);
                                sendRoomBroadcast(id, name, created);
                            }

                            if (shouldSendNotification) {
                                sendRoomNotification(id, name, sender);
                            }

                            /*
                            if (shouldSyncAddDb) {
                                updateRoomSyncData(id, created);
                                sqlHelper.addRoom(id, name, created);
                            }

                            if (shouldSendNotification) {
                                sendRoomNotification(id, name, sender);
                            }
                            else if (shouldSendBroadcast) {
                                sendRoomBroadcast(id, name, created);
                            }
                            */

                            /*
                            if (senderId != userId) {
                                // is a room user is a part of
                                // sender is not user
                                if (onRoomList) {
                                    updateRoomSyncData(id, created);
                                    sqlHelper.addRoom(id, name, created);
                                    sendRoomBroadcast(id, name, created);
                                    // DONE: db+broadcast
                                    Log.d(Constants.APP_NAME, "FWSL: new room | db+list");
                                }
                                else {
                                    updateRoomSyncData(id, created);
                                    sqlHelper.addRoom(id, name, created);
                                    sendRoomNotification(id, name, sender);
                                    // DONE: make notification, add to db
                                    Log.d(Constants.APP_NAME, "FWSL: new room | notif+db");
                                }
                            }

                            else if (!senderUnique.equals(usernameUnique)) {
                                // is a room user is a part of
                                // sender is user, but different client signature
                                if (onRoomList) {
                                    updateRoomSyncData(id, created);
                                    sqlHelper.addRoom(id, name, created);
                                    sendRoomBroadcast(id, name, created);
                                    // DONE: db+broadcast
                                    Log.d(Constants.APP_NAME, "FWSL: new room by user | db+list");
                                }
                                else {
                                    updateRoomSyncData(id, created);
                                    sqlHelper.addRoom(id, name, created);
                                    // DONE: add to db
                                    Log.d(Constants.APP_NAME, "FWSL: new room by user | db");
                                }
                            }
                            else { Log.d(Constants.APP_NAME, "FWSL: new room by user | SAMECLIENT!"); }
                            */
                        }
                        // else { Log.d(Constants.APP_NAME, "FWSL: new room | USERNOTINROOM!"); }
                    }
                    else if (notificationObjectType.equals("message")) {
                        // TODO: do message stuff here
                        JSONArray participantsJson = attr.getJSONArray("room_participants");

                        // db data
                        int roomId = attr.getInt("room");
                        String content = attr.getString("content");
                        long timeCreated = attr.getLong("time");

                        Location location = null;
                        float latitude = -1,
                              longitude = -1;
                        if (!attr.isNull("location")) {
                            JSONObject locationJson = attr.getJSONObject("location");
                            latitude = (float)locationJson.getDouble("latitude");
                            longitude = (float)locationJson.getDouble("longitude");
                            location = new Location(latitude, longitude);
                        }

                        MessageFile file = null;
                        String hash = null, name = null;
                        if (!attr.isNull("file")) {
                            JSONObject fileJson = attr.getJSONObject("file");
                            hash = fileJson.getString("hash");
                            name = fileJson.getString("name");
                            file = new MessageFile(hash, name);
                        }

                        Message message = new Message(sender, content, timeCreated, location, file);

                        // sync data
                        int messageId = attr.getInt("message_id");

                        // notif data
                        // TODO: FIX THIS!
                        String roomName = "test";

                        if (jsonArrayContains(participantsJson, userId)) {
                            boolean anyOtherDeviceThanThis = !senderUnique.equals(usernameUnique);
                            boolean differentUser = senderId != userId;
                            boolean onMessageRoom = roomId == currentRoom;

                            boolean shouldSyncAddDb = anyOtherDeviceThanThis;
                            boolean shouldSendMessageBroadcasts = anyOtherDeviceThanThis && bound;
                            boolean shouldSendMessageNotification = differentUser && (!onMessageRoom && !onRoomList);

                            if (shouldSyncAddDb) {
                                updateMessageSyncData(messageId, timeCreated);
                                sqlHelper.addMessage(roomId, message);
                            }

                            if (shouldSendMessageBroadcasts) {
                                sendMessageBroadcast(
                                        sender,
                                        content,
                                        timeCreated,
                                        location == null,
                                        latitude,
                                        longitude,
                                        file == null,
                                        hash,
                                        name
                                );
                                sendMessageLiteBroadcast(
                                        roomId,
                                        content,
                                        timeCreated
                                );
                            }

                            if (shouldSendMessageNotification) {
                                sendMessageNotification(roomId, roomName, sender);
                            }

                            /*
                            boolean shouldSync = anyOtherDeviceThanThis;
                            boolean shouldAddToDb = anyOtherDeviceThanThis;
                            boolean shouldSendMessageBroadcast = anyOtherDeviceThanThis && bound;
                            boolean shouldSendMessageLiteBroadcast = anyOtherDeviceThanThis && bound;
                            boolean shouldSendMessageNotification = differentUser && (!onMessageRoom && !onRoomList);
                            */


                            /*
                            if (senderId != userId) {
                                if (roomId == currentRoom) {
                                    updateMessageSyncData(messageId, timeCreated);
                                    sqlHelper.addMessage(roomId, message);
                                    sendMessageBroadcast(
                                            sender,
                                            content,
                                            timeCreated,
                                            location == null,
                                            latitude,
                                            longitude,
                                            file == null,
                                            hash,
                                            name
                                    );
                                }
                                else if (onRoomList) {
                                    updateMessageSyncData(messageId, timeCreated);
                                    sqlHelper.addMessage(roomId, message);
                                    sendMessageLiteBroadcast(
                                            roomId,
                                            content,
                                            timeCreated
                                    );
                                    // TODO: db+broadcast
                                    Log.d(Constants.APP_NAME, "FWSL: new message | db+list");
                                }
                                else {
                                    // is a room user is a part of
                                    // sender is not user
                                    // no list activity is visible
                                    updateMessageSyncData(messageId, timeCreated);
                                    sqlHelper.addMessage(roomId, message);
                                    sendMessageNotification(roomId, roomName, sender);
                                    // TODO: notification+db
                                    Log.d(Constants.APP_NAME, "FWSL: new message | notif+db");
                                }
                            }
                            else if (!senderUnique.equals(usernameUnique)) {
                                // is a room user is a part of
                                // sender is user, but different client signature
                                if (roomId == currentRoom) {
                                    updateMessageSyncData(messageId, timeCreated);
                                    sqlHelper.addMessage(roomId, message);
                                    sendMessageBroadcast(
                                            sender,
                                            content,
                                            timeCreated,
                                            location == null,
                                            latitude,
                                            longitude,
                                            file == null,
                                            hash,
                                            name
                                    );
                                }
                                else if (onRoomList) {
                                    updateMessageSyncData(messageId, timeCreated);
                                    sqlHelper.addMessage(roomId, message);
                                    sendMessageLiteBroadcast(
                                            roomId,
                                            content,
                                            timeCreated
                                    );
                                    // TODO: db+broadcast
                                    Log.d(Constants.APP_NAME, "FWSL: new message by user | db+list");
                                }
                                else {
                                    // is a room user is a part of
                                    // no list activity visible
                                    updateMessageSyncData(messageId, timeCreated);
                                    sqlHelper.addMessage(roomId, message);
                                    // TODO: add to db
                                    Log.d(Constants.APP_NAME, "FWSL: new message by user | db");
                                }
                            }
                            else { Log.d(Constants.APP_NAME, "FWSL: new message by user | SAMECLIENT!"); }
                            */
                        }
                        else { Log.d(Constants.APP_NAME, "FWSL: new message | USERNOTINROOM!"); }
                    }
                }
                else { Log.d(Constants.APP_NAME, "FWSL: unexpected WS message type"); }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class LocalBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.APP_NAME, "WebSocketService onCreate");

        alreadyStarted = false;
        bound = false;
        currentRoom = NOT_ON_MESSAGE_ACTIVITY;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.APP_NAME, "WebSocketService onStartCommand");

        if (!alreadyStarted) {
            prefs = new SharedPreferencesHelper(this);

            usernameUnique = String.format(
                    "%s_%s",

                    prefs.getString(SharedPreferencesHelper.KEY_USERNAME),
                    generateRandomString(4)
            );

            sqlHelper = new SqlHelper(this);

            listener = new FlackWebSocketListener();

            if (!WebSocketSingleton.create()) {
                int roomId = prefs.getInt(SharedPreferencesHelper.KEY_SYNC_ROOMID);
                long roomTime = prefs.getLong(SharedPreferencesHelper.KEY_SYNC_ROOMTIME);

                int messageId = prefs.getInt(SharedPreferencesHelper.KEY_SYNC_MESSAGEID);
                long messageTime = prefs.getLong(SharedPreferencesHelper.KEY_SYNC_MESSAGETIME);

                if (roomId != -1 && messageId != -1) {
                    if (roomTime > messageTime) {
                        messageId = -1;
                    }
                    else {
                        roomId = -1;
                    }
                }

                WebSocketSingleton.initialize(
                        prefs.getString(SharedPreferencesHelper.KEY_SERVERADDR),
                        prefs.getString(SharedPreferencesHelper.KEY_AUTHTOKEN),
                        roomId,
                        messageId,
                        usernameUnique,
                        listener
                );
                WebSocketSingleton.create();
            }
        }
        alreadyStarted = true;

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        bound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        bound = false;
        currentRoom = NOT_ON_MESSAGE_ACTIVITY;

        // will return false, causing the currently unimplemented onRebind method not to be fired
        return super.onUnbind(intent);
    }

    private void sendRoomNotification(int roomId, String roomName, String creator) {
        showNotification(
                roomId,
                String.format("%s created a new room called %s", creator, roomName)
        );
    }

    private void sendMessageNotification(int roomId, String roomName, String creator) {
        showNotification(
                roomId,
                String.format("%s sent a new message to room %s", creator, roomName)
        );
    }

    private void showNotification(int roomId, String message) {
        Log.d(Constants.APP_NAME, "Flack|WebSocketService|showNotification DEFALLONLY");
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra(MessageActivity.KEY_ROOMID, roomId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // TODO: make an icon
        Notification notification = new Notification.Builder(this)
                .setContentTitle(Constants.APP_NAME)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private void sendRoomBroadcast(int id, String name, long created) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.APP_PKG_NAME);

        broadcast.putExtra(KEY_OBJECTTYPE, TYPE_ROOM);
        broadcast.putExtra(KEY_ROOM_SERVERID, id);
        broadcast.putExtra(KEY_ROOM_NAME, name);
        broadcast.putExtra(KEY_ROOM_TIMECREATED, created);

        sendBroadcast(broadcast);
    }

    private void sendMessageBroadcast(String sender, String content, long created,
                                      boolean isLocationNull, float locationLatitude, float locationLongitude,
                                      boolean isFileNull, String fileHash, String fileName) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.APP_PKG_NAME);

        broadcast.putExtra(KEY_OBJECTTYPE, TYPE_MESSAGE);
        broadcast.putExtra(KEY_MESSAGE_SENDER, sender);
        broadcast.putExtra(KEY_MESSAGE_CONTENT, content);
        broadcast.putExtra(KEY_MESSAGE_TIMECREATED, created);

        broadcast.putExtra(KEY_MESSAGE_LOCATION_ISNULL, isLocationNull);
        if (!isLocationNull) {
            broadcast.putExtra(KEY_MESSAGE_LOCATION_LATITUDE, locationLatitude);
            broadcast.putExtra(KEY_MESSAGE_LOCATION_LONGITUDE, locationLongitude);
        }

        broadcast.putExtra(KEY_MESSAGE_FILE_ISNULL, isFileNull);
        if (!isFileNull) {
            broadcast.putExtra(KEY_MESSAGE_FILE_HASH, fileHash);
            broadcast.putExtra(KEY_MESSAGE_FILE_NAME, fileName);
        }

        sendBroadcast(broadcast);
    }

    private void sendMessageLiteBroadcast(int roomId, String messageText, long messageTime) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.APP_PKG_NAME);

        broadcast.putExtra(KEY_OBJECTTYPE, TYPE_MESSAGELITE);
        broadcast.putExtra(KEY_MESSAGELITE_ROOMID, roomId);
        broadcast.putExtra(KEY_MESSAGELITE_TEXT, messageText);
        broadcast.putExtra(KEY_MESSAGELITE_TIMECREATED, messageTime);

        sendBroadcast(broadcast);
    }

    private void updateRoomSyncData(int id, long time) {
        long oldRoomTime = prefs.getLong(SharedPreferencesHelper.KEY_SYNC_ROOMTIME);

        if (time > oldRoomTime) {
            prefs.save(SharedPreferencesHelper.KEY_SYNC_ROOMID, id);
            prefs.save(SharedPreferencesHelper.KEY_SYNC_ROOMTIME, time);
        }
    }

    private void updateMessageSyncData(int id, long time) {
        long oldMessageTime = prefs.getLong(SharedPreferencesHelper.KEY_SYNC_MESSAGETIME);

        if (time > oldMessageTime) {
            prefs.save(SharedPreferencesHelper.KEY_SYNC_MESSAGEID, id);
            prefs.save(SharedPreferencesHelper.KEY_SYNC_MESSAGETIME, time);
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

    public void setCurrentRoom(int roomId) {
        currentRoom = roomId;
    }
}
