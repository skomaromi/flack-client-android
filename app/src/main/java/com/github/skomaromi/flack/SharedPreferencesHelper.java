package com.github.skomaromi.flack;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
    public static final String KEY_USERNAME = "user_name";
    public static final String KEY_USERID = "user_server_id";
    public static final String KEY_SERVERADDR = "server_address";
    public static final String KEY_AUTHTOKEN = "auth_token";
    public static final String KEY_SYNC_ROOMID = "sync_roomid";
    public static final String KEY_SYNC_ROOMTIME = "sync_roomtime";
    public static final String KEY_SYNC_MESSAGEID = "sync_messageid";
    public static final String KEY_SYNC_MESSAGETIME = "sync_messagetime";

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    public SharedPreferencesHelper(Context context) {
        mPreferences = context.getSharedPreferences(
                Constants.APP_PKG_NAME,
                Context.MODE_PRIVATE
        );
        mEditor = mPreferences.edit();
    }

    public void save(String key, String value) {
        mEditor.putString(key, value).apply();
    }

    public void save(String key, int value) {
        mEditor.putInt(key, value).apply();
    }

    public void save(String key, long value) {
        mEditor.putLong(key, value).apply();
    }

    public String getString(String key) {
        return mPreferences.getString(key, null);
    }

    public int getInt(String key) {
        return mPreferences.getInt(key, -1);
    }

    public long getLong(String key) {
        return mPreferences.getLong(key, -1);
    }
}
