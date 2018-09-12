package com.github.skomaromi.flack;

import android.content.Context;

import com.rabtman.wsmanager.WsManager;
import com.rabtman.wsmanager.listener.WsStatusListener;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketSingleton {
    public static final String PROTO = "ws";

    private static WebSocketSingleton mInstance;

    private static String mAddress, mToken, mUsernameUnique;
    private static int mRoomSince, mMessageSince;
    private static String mUrl;
    private static WsStatusListener mListener;
    private static WsManager mWsManager;

    public static boolean create(Context context) {
        if (mInstance == null) {
            if (mAddress == null && mToken == null && mListener == null) {
                return false;
            }
            else {
                OkHttpClient client = new OkHttpClient().newBuilder()
                                              .pingInterval(15, TimeUnit.SECONDS)
                                              .retryOnConnectionFailure(true)
                                              .build();

                mWsManager = new WsManager.Builder(context)
                                     .wsUrl(mUrl)
                                     .needReconnect(true)
                                     .client(client)
                                     .build();

                mWsManager.setWsStatusListener(mListener);

                mWsManager.startConnect();

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
                                  WsStatusListener listener) {
        mAddress = address;
        mToken = token;
        mRoomSince = room;
        mMessageSince = message;
        mUsernameUnique = usernameUnique;
        mListener = listener;

        mUrl = String.format(
                Locale.ENGLISH,
                "%s://%s:%d/%s/%d/%d/",

                PROTO,
                mAddress,
                Constants.SERVER_PORT,
                mToken,
                mRoomSince,
                mMessageSince
        );
    }

    public static void send(String message) {
        mWsManager.sendMessage(message);
    }
}
