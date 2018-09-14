package com.github.skomaromi.flack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import butterknife.ButterKnife;

public class StartActivity extends AppCompatActivity {
    private BackgroundInitTask initTask;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        ButterKnife.bind(this);

        prefs = new SharedPreferencesHelper(this);
        init();
    }

    private void init() {
        initTask = new BackgroundInitTask(this);
        initTask.execute();
    }

    private class BackgroundInitTask extends AsyncTask<Void, Void, Integer> {
        public static final int RETCODE_OK = 0;
        public static final int RETCODE_ERROR = 1;

        private StartActivity activity;

        public BackgroundInitTask(StartActivity activity) {
            this.activity = activity;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            logMessage("checking authentication data...");
            boolean hasToken = false;
            String authToken = prefs.getString(SharedPreferencesHelper.KEY_AUTHTOKEN);
            hasToken = authToken != null;

            logMessage("checking if connected to a wireless network...");
            boolean connectedToWifi = false;
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            connectedToWifi = wifi.isConnected();

            logMessage("testing connection to server...");
            boolean serverReachable = false;
            if (connectedToWifi) {
                String serverAddr = prefs.getString(SharedPreferencesHelper.KEY_SERVERADDR);
                if (serverAddr != null) {
                    if (FlackApi.testConnection(serverAddr)) {
                        serverReachable = true;
                    }
                }
                else {
                    if (FlackApi.testConnection(Constants.SERVER_DEFAULT_ADDR)) {
                        prefs.save(
                                SharedPreferencesHelper.KEY_SERVERADDR,
                                Constants.SERVER_DEFAULT_ADDR
                        );
                        serverReachable = true;
                    }
                    else if (FlackApi.testConnection(Constants.SERVER_DEFAULT_ADDR_ALT)) {
                        prefs.save(
                                SharedPreferencesHelper.KEY_SERVERADDR,
                                Constants.SERVER_DEFAULT_ADDR_ALT
                        );
                        serverReachable = true;
                    }
                }
            }

            logMessage("checking if service running...");
            boolean serviceRunning = false;
            serviceRunning = FlackApplication.isServiceRunning();

            boolean shouldWarnClose = !hasToken && !connectedToWifi;
            boolean shouldShowServerInputActivity = !hasToken && connectedToWifi && !serverReachable;
            boolean shouldShowAuthActivity = !hasToken && connectedToWifi && serverReachable;
            boolean shouldShowRoomsActivity = hasToken || serviceRunning;

            if (shouldWarnClose) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(activity);
                        builder.setMessage("Device not connected to a " +
                                                   "wireless network. " +
                                                   "Wireless connection is " +
                                                   "necessary to proceed.");
                        builder.setPositiveButton(
                                "Quit", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        dialog.dismiss();
                                        closeParentActivity();
                                    }
                                });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
                return RETCODE_ERROR;
            }
            else if (shouldShowServerInputActivity) {
                startServerInputActivity();
                return RETCODE_ERROR;
            }
            else if (shouldShowAuthActivity) {
                startAuthActivity();
                return RETCODE_ERROR;
            }
            else if (shouldShowRoomsActivity) {
                startRoomsActivity();
                return RETCODE_OK;
            }

            // this should not be reachable, so returning RETCODE_ERROR
            return RETCODE_ERROR;
        }

        private void logMessage(String message) {
            Log.d(
                    Constants.APP_NAME,
                    String.format("BackgroundInitTask: %s", message)
            );
        }

        private void closeParentActivity() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.finish();
                }
            });
        }
    }

    private void startServerInputActivity() {
        Intent serverInputActivity = new Intent(this, ServerInputActivity.class);
        startActivityForResult(serverInputActivity, Constants.REQCODE_ACTIVITY_SERVERINPUT);
    }

    private void startAuthActivity() {
        Intent authActivity = new Intent(this, AuthActivity.class);
        startActivityForResult(authActivity, Constants.REQCODE_ACTIVITY_AUTH);
    }

    private void startRoomsActivity() {
        Intent roomsActivity = new Intent(this, RoomActivity.class);
        startActivity(roomsActivity);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == Constants.REQCODE_ACTIVITY_SERVERINPUT) {
            if (resultCode == Activity.RESULT_OK) {
                init();
            }
            else {
                // user exited before providing valid input
                finish();
            }
        }
        else if (requestCode == Constants.REQCODE_ACTIVITY_AUTH) {
            if (resultCode == Activity.RESULT_OK) {
                init();
            }
            else {
                finish();
            }
        }
    }
}
