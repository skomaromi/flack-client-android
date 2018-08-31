package com.github.skomaromi.flack;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        String authToken = getAuthenticationToken();

        if (authToken != null) {
            showRoomsActivity(authToken);
        }
        else {
            showLoginActivity();
        }
    }

    private String getAuthenticationToken() {
        SharedPreferences prefs = getSharedPreferences(
                Constants.APP_PKG_NAME,
                Context.MODE_PRIVATE
        );

        return prefs.getString(Constants.SP_KEY_AUTHTOKEN, null);
    }

    private void showRoomsActivity(String authToken) {
        Log.d(Constants.APP_NAME, "showRoomsActivity(): NOT IMPLEMENTED!");
    }

    private void showLoginActivity() {
        Log.d(Constants.APP_NAME, "showLoginActivity(): NOT IMPLEMENTED!");
    }
}
