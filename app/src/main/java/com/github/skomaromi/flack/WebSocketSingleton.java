package com.github.skomaromi.flack;

import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketSingleton {
    public static final String PROTO = "ws";

    private static WebSocketSingleton mInstance;

    private static String mAddress, mToken, mUsernameUnique;
    private static int mRoomSince, mMessageSince;
    private static WebSocketListener mListener;
    private static WebSocket mSocket;

    private WebSocketSingleton() {
        OkHttpClient client = new OkHttpClient();

        String url = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s/%d/%d/",

                PROTO,
                mAddress,
                Constants.SERVER_PORT,
                mToken,
                mRoomSince,
                mMessageSince
        );

        Request request = new Request.Builder()
                                  .url(url)
                                  .build();

        mSocket = client.newWebSocket(request, mListener);
    }

    public static boolean create() {
        if (mInstance == null) {
            if (mAddress == null && mToken == null && mListener == null) {
                return false;
            }
            else {
                mInstance = new WebSocketSingleton();
                return true;
            }
        }
        else {
            return true;
        }
    }

    public static void initialize(String address, String token,
                                  int room, int message,
                                  String usernameUnique,
                                  WebSocketListener listener) {
        mAddress = address;
        mToken = token;
        mRoomSince = room;
        mMessageSince = message;
        mUsernameUnique = usernameUnique;
        mListener = listener;
    }

    public static WebSocketSingleton getInstance() {
        return mInstance;
    }

    public void send(String message) {
        mSocket.send(message);
    }
}
