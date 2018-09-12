package com.github.skomaromi.flack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.rabtman.wsmanager.listener.WsStatusListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import okhttp3.Response;

public class WebSocketService extends Service {
    public static final int TYPE_ROOM = 1;
    public static final int TYPE_MESSAGE = 2;
    public static final int TYPE_MESSAGELITE = 3;

    public static final String KEY_OBJECTTYPE = "type";

    public static final String KEY_ROOM_SERVERID = "server_id";
    public static final String KEY_ROOM_NAME = "name";
    public static final String KEY_ROOM_TIMECREATED = "time_created";

    public static final String KEY_MESSAGE_ROOMID = "room_id";
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

    private final IBinder mBinder = new WebSocketBinder();
    private SharedPreferencesHelper prefs;
    private FlackWsStatusListener listener;
    private SqlHelper sqlHelper;
    private String usernameUnique;
    private boolean alreadyStarted;

    private class FlackWsStatusListener extends WsStatusListener {
        private int userId;

        public FlackWsStatusListener() {
            userId = prefs.getInt(SharedPreferencesHelper.KEY_USERID);
            Log.d(Constants.APP_NAME, String.format("FWSL: userId: %d", userId));
        }

        @Override
        public void onOpen(Response response) {
            super.onOpen(response);
            Log.d(Constants.APP_NAME, "FWSL: websocket open!");
        }

        @Override
        public void onFailure(Throwable t, Response response) {
            super.onFailure(t, response);
            if (t != null) {
                t.printStackTrace();
            }
            Log.d(Constants.APP_NAME, "FWSL: websocket failure");
        }

        @Override
        public void onClosed(int code, String reason) {
            super.onClosed(code, reason);
            Log.d(Constants.APP_NAME, "FWSL: websocket disconnected!");
        }


        @Override
        public void onMessage(String text) {
            Log.d(Constants.APP_NAME, "FlackWsStatusListener: okay, we got " +
                                              "something");
            Log.d(Constants.APP_NAME, String.format("FWSL: <msg>%s</msg>", text));

            try {
                JSONObject textJson = new JSONObject(text);
                String textType = textJson.getString("type");

                //
                // responses
                //
                // message example
                // {
                //    "type":"response",
                //    "attr":{
                //       "object":"message",
                //       "content":"Lorem ipsum dolor sit amet",
                //       "file":{
                //          "name":"pwbg.jpg",
                //          "hash":"QmVAD7ZEdFWBGypKCT9JatP7x2jYwY4mm9jReUnpkwEGFy",
                //          "url":"https://ipfs.io/ipfs/QmVAD7ZEdFWBGypKCT9JatP7x2jYwY4mm9jReUnpkwEGFy/pwbg.jpg"
                //       },
                //       "room":17,
                //       "room_name":"Room example name",
                //       "room_participants":[
                //          1,
                //          2,
                //          3
                //       ],
                //       "sender":"admin",
                //       "sender_id":1,
                //       "sender_unique":"admin_k52w",
                //       "location":{
                //          "latitude":45.55397415161133,
                //          "longitude":18.67446517944336
                //       },
                //       "message_id":276,
                //       "time":1536790493470.4531
                //    }
                // }

                //
                // response
                //
                if (textType.equals("response")) {
                    // TODO: do response stuff here
                    // if response, store and UI
                    JSONObject attr = textJson.getJSONObject("attr");

                    String responseObjectType = attr.getString("object");

                    if (responseObjectType.equals("message")) {
                        // db data
                        int messageId = attr.getInt("message_id");
                        int roomId = attr.getInt("room");
                        String sender = attr.getString("sender");
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

                        boolean messageAlreadyReceived;
                        messageAlreadyReceived = !sqlHelper.addMessage(messageId, roomId, message);

                        if (!messageAlreadyReceived) {
                            updateMessageSyncData(
                                    messageId,
                                    timeCreated
                            );
                            sendMessageBroadcast(
                                    roomId,
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
                                    message.toString(),
                                    timeCreated
                            );
                        }
                    }
                }


                //
                // notifications
                //
                // room example
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

                // message example
                // {
                //    "type":"notification",
                //    "attr":{
                //       "object":"message",
                //       "content":"Lorem ipsum dolor sit amet",
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

                //
                // notification
                //
                else if (textType.equals("notification")) {
                    JSONObject attr = textJson.getJSONObject("attr");

                    String sender = attr.getString("sender");
                    int senderId = attr.getInt("sender_id");
                    String senderUnique = attr.getString("sender_unique");

                    boolean onRoomList = FlackApplication.checkIsOnRoomList();
                    int currentRoom = FlackApplication.getCurrentRoom();

                    String notificationObjectType = attr.getString("object");

                    if (notificationObjectType.equals("room")) {
                        JSONArray participantsJson = attr.getJSONArray("participants");

                        // room db data
                        int id = attr.getInt("id");
                        String name = attr.getString("name");
                        long created = attr.getLong("time");

                        if (jsonArrayContains(participantsJson, userId)) {
                            boolean isFromAnyOtherDevice = !senderUnique.equals(usernameUnique);
                            boolean differentUser = senderId != userId;

                            boolean roomAlreadyReceived = true;
                            if (isFromAnyOtherDevice) {
                                roomAlreadyReceived = !sqlHelper.addRoom(id, name, created);
                            }

                            boolean shouldDoSyncBroadcast = !roomAlreadyReceived;
                            boolean shouldSendNotification = !roomAlreadyReceived && differentUser && !onRoomList;

                            if (shouldDoSyncBroadcast) {
                                updateRoomSyncData(id, created);
                                sendRoomBroadcast(id, name, created);
                            }

                            if (shouldSendNotification) {
                                sendRoomNotification(id, name, sender);
                            }
                        }
                    }
                    else if (notificationObjectType.equals("message")) {
                        JSONArray participantsJson = attr.getJSONArray("room_participants");

                        // db data
                        int messageId = attr.getInt("message_id");
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

                        // notif data
                        String roomName = attr.getString("room_name");

                        if (jsonArrayContains(participantsJson, userId)) {
                            boolean isFromAnyOtherDevice = !senderUnique.equals(usernameUnique);
                            boolean differentUser = senderId != userId;
                            boolean onMessageRoom = roomId == currentRoom;

                            boolean messageAlreadyReceived = true;
                            if (isFromAnyOtherDevice) {
                                messageAlreadyReceived = !sqlHelper.addMessage(messageId, roomId, message);
                            }

                            boolean shouldDoSyncBroadcast = !messageAlreadyReceived;
                            boolean shouldSendMessageNotification = !messageAlreadyReceived && differentUser && !onMessageRoom && !onRoomList;

                            if (shouldDoSyncBroadcast) {
                                updateMessageSyncData(messageId, timeCreated);
                                sendMessageBroadcast(
                                        roomId,
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
                                        message.toString(),
                                        timeCreated
                                );
                            }

                            if (shouldSendMessageNotification) {
                                sendMessageNotification(roomId, roomName, sender);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class WebSocketBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.APP_NAME, "WebSocketService onCreate");

        alreadyStarted = false;
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

            listener = new FlackWsStatusListener();

            if (!WebSocketSingleton.create(this)) {
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
                WebSocketSingleton.create(this);
            }
        }
        alreadyStarted = true;

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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
                .setAutoCancel(true)
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

    private void sendMessageBroadcast(int roomId, String sender, String content, long created,
                                      boolean isLocationNull, float locationLatitude, float locationLongitude,
                                      boolean isFileNull, String fileHash, String fileName) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.APP_PKG_NAME);

        broadcast.putExtra(KEY_OBJECTTYPE, TYPE_MESSAGE);
        broadcast.putExtra(KEY_MESSAGE_ROOMID, roomId);
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

    public void sendMessage(String content, int fileId, int roomId, Location location) {
        String message = "";
        // example:
        //  {
        //      type: "create",
        //      attr: {
        //          object: "message",
        //          sender_unique: "<username>_aH4x",
        //          content: "lorem ipsum dolor sit amet",
        //          file: <null or [1-9][0-9]*>
        //          room: <any valid room pk/id>,
        //          location: {
        //              latitude: <some float>,
        //              longitude: <some float>
        //          } <or null>
        //      }
        //  }

        try {
            JSONObject messageJson = new JSONObject();
            messageJson.put("type", "create");

            JSONObject attrJson = new JSONObject();
            attrJson.put("object", "message");
            attrJson.put("sender_unique", usernameUnique);
            attrJson.put("content", content);

            if (fileId != -1) {
                attrJson.put("file", fileId);
            }
            else {
                attrJson.put("file", null);
            }

            attrJson.put("room", roomId);

            JSONObject locationJson;
            if (location != null) {
                locationJson = new JSONObject();
                locationJson.put("latitude", location.getLatitude());
                locationJson.put("longitude", location.getLongitude());
            }
            else {
                locationJson = null;
            }
            attrJson.put("location", locationJson);

            messageJson.put("attr", attrJson);

            message = messageJson.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        WebSocketSingleton.send(message);
    }
}
