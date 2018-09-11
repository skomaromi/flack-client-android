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
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StartActivity extends AppCompatActivity {
    @BindView(R.id.start_tv_progress) TextView progressLabel;

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
            /* TODO: allow listening for connection changes
             * > check if has authtoken
             * > if yes, offer to show old messages
             *   > on reconnect sync via API and wait for WS events
             */

            //
            // step 1: check connection
            //
            postProgress("testing connection...");

            ConnectivityManager connectivityManager =
                    (ConnectivityManager)
                            getSystemService(CONNECTIVITY_SERVICE);

            NetworkInfo wifi =
                    connectivityManager.getNetworkInfo(
                            ConnectivityManager.TYPE_WIFI
                    );

            // if wifi off, show dialog and close app on OK
            if (!wifi.isConnected()) {
                logMessage("(err) no wifi connection");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(activity);
                        builder.setMessage("Device not connected to a " +
                                                   "wireless network. " +
                                                   "Wireless connection is " +
                                                   "necessary to use Flack.");
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

            //
            // step 2: check server
            //
            postProgress("checking stored server settings...");

            String serverAddr = prefs.getString(
                    SharedPreferencesHelper.KEY_SERVERADDR
            );
            if (serverAddr != null) {
                postProgress("testing connection to server...");

                if (!FlackApi.testConnection(serverAddr)) {
                    // show dialog
                    final String addr = serverAddr;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder =
                                    new AlertDialog.Builder(activity);
                            builder.setMessage("Cannot reach currently " +
                                                       "configured server. " +
                                                       "Do you want to quit " +
                                                       "application or " +
                                                       "modify server address?"
                            );
                            builder.setNegativeButton(
                                    "Quit",
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    dialog.dismiss();
                                    closeParentActivity();
                                }
                            });
                            builder.setPositiveButton(
                                    "Modify",
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    dialog.dismiss();
                                    showServerInputActivity(addr);
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                    return RETCODE_ERROR;
                }
            }
            else {
                postProgress("testing default settings...");

                if (FlackApi.testConnection(Constants.SERVER_DEFAULT_ADDR)) {
                    logMessage("storing primary default server address...");
                    prefs.save(
                            SharedPreferencesHelper.KEY_SERVERADDR,
                            Constants.SERVER_DEFAULT_ADDR
                    );
                    serverAddr = Constants.SERVER_DEFAULT_ADDR;
                }
                else if (FlackApi.testConnection(
                        Constants.SERVER_DEFAULT_ADDR_ALT
                )) {
                    logMessage("storing secondary default server address...");
                    prefs.save(
                            SharedPreferencesHelper.KEY_SERVERADDR,
                            Constants.SERVER_DEFAULT_ADDR_ALT
                    );

                    serverAddr = Constants.SERVER_DEFAULT_ADDR_ALT;
                }
                else {
                    logMessage(
                            "both addresses failed, asking user for manual " +
                                    "input..."
                    );
                    showServerInputActivity(null);
                    return RETCODE_ERROR;
                }
            }

            //
            // step 3: authentication, room list
            //
            postProgress("checking authentication data...");

            String authToken = prefs.getString(
                    SharedPreferencesHelper.KEY_AUTHTOKEN
            );
            if (authToken != null) {
                showRoomsActivity(serverAddr, authToken);
            }
            else {
                showAuthActivity(serverAddr);
                return RETCODE_ERROR;
            }

            postProgress("everything okay!");
            return RETCODE_OK;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(Constants.APP_NAME, "BackgroundInitTask::onPostExecute");
        }

        private void postProgress(String message) {
            setProgressLabelText(message);
            logMessage(message);
        }

        private void logMessage(String message) {
            Log.d(
                    Constants.APP_NAME,
                    String.format("BackgroundInitTask: %s", message)
            );
        }

        private void setProgressLabelText(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressLabel.setText(text);
                }
            });
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

    private void showServerInputActivity(String address) {
        Intent serverInputActivity = new Intent(
                this, ServerInputActivity.class
        );

        if (address != null) {
            serverInputActivity.putExtra(
                    ServerInputActivity.KEY_ADDRESS,
                    address
            );
        }

        startActivityForResult(
                serverInputActivity,
                Constants.REQCODE_ACTIVITY_SERVERINPUT
        );
    }

    private void showAuthActivity(String address) {
        Intent authActivity = new Intent(this, AuthActivity.class);
        authActivity.putExtra(AuthActivity.KEY_ADDRESS, address);

        startActivityForResult(authActivity, Constants.REQCODE_ACTIVITY_AUTH);
    }

    private void showRoomsActivity(String address, String token) {
        Intent roomsActivity = new Intent(this, RoomActivity.class);
        roomsActivity.putExtra(RoomActivity.KEY_ADDRESS, address);
        roomsActivity.putExtra(RoomActivity.KEY_AUTHTOKEN, token);

        startActivity(roomsActivity);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == Constants.REQCODE_ACTIVITY_SERVERINPUT) {
            if (resultCode == Activity.RESULT_OK) {
                // store returned data
                Log.d(
                        Constants.APP_NAME,
                        "ServerInputActivity returned RESULT_OK"
                );

                String address = data.getStringExtra(
                        ServerInputActivity.KEY_ADDRESS
                );

                prefs.save(SharedPreferencesHelper.KEY_SERVERADDR, address);

                init();
            }
            else {
                // user manually exited the ServerInputActivity
                finish();
            }
        }
        else if (requestCode == Constants.REQCODE_ACTIVITY_AUTH) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(Constants.APP_NAME, "AuthActivity returned RESULT_OK");

                String token = data.getStringExtra(AuthActivity.KEY_AUTHTOKEN);

                prefs.save(SharedPreferencesHelper.KEY_AUTHTOKEN, token);

                init();
            }
            else {
                finish();
            }
        }
    }

    // TODO: remove if not needed
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(Constants.APP_NAME, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Constants.APP_NAME, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Constants.APP_NAME, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.APP_NAME, "onResume");
    }
}
