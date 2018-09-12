package com.github.skomaromi.flack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class ServerInputActivity extends AppCompatActivity {
    @BindView(R.id.srvinput_btn_connect) Button connectButton;
    @BindView(R.id.srvinput_et_address) EditText addressField;

    public static final String KEY_ADDRESS = "address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_input);
        ButterKnife.bind(this);

        connectButton.setEnabled(false);

        Intent data = getIntent();
        handleIntentData(data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void handleIntentData(Intent data) {
        if (data != null) {
            if (data.hasExtra(KEY_ADDRESS)) {
                String address = data.getStringExtra(KEY_ADDRESS);
                addressField.setText(address);
            }
        }
    }

    @OnTextChanged(R.id.srvinput_et_address)
    public void textFieldChanged() {
        String address;

        address = addressField.getText().toString();

        /*
         * as per https://stackoverflow.com/q/3523028:
         *  series of labels separated with dots, each between 1 and 63
         *  characters long. allowed characters are digits, uppercase and
         *  lowercase ASCII letters and a hyphen. no hyphens at the beginning
         *  or end of labels allowed.
         *
         *  entire hostname at most 253 characters long
         */
        String addressRegex =
                "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
                        "(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

        boolean shouldEnable =
                !address.isEmpty() &&
                address.matches(addressRegex) &&
                address.length() < 253;

        connectButton.setEnabled(shouldEnable);
    }

    @OnClick(R.id.srvinput_btn_connect)
    public void connect() {
        BackgroundConnectTask t = new BackgroundConnectTask(this);
        t.execute();
    }

    private class BackgroundConnectTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;

        public BackgroundConnectTask(ServerInputActivity activity) {
            progressDialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Connecting...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            String address;

            // connect button is disabled if the address field value is
            // invalid, so this string is certainly valid
            address = addressField.getText().toString();

            if (FlackApi.testConnection(address)) {
                return address;
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String address) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            connectTaskReturnHandler(address);
        }
    }

    private void connectTaskReturnHandler(String address) {
        if (address != null) {
            Log.d(Constants.APP_NAME, "(ok) retrieved valid server response");

            Intent data = new Intent();

            data.putExtra(KEY_ADDRESS, address);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        else {
            Log.d(Constants.APP_NAME, "(err) could not reach server");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    "Incorrect address provided or no connection available. " +
                            "Please try again."
            );

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
