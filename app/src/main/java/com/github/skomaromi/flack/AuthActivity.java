package com.github.skomaromi.flack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

public class AuthActivity extends AppCompatActivity {
    @BindView(R.id.auth_et_loginusername) EditText loginUsernameField;
    @BindView(R.id.auth_et_loginpassword) EditText loginPasswordField;
    @BindView(R.id.auth_btn_login) Button loginButton;

    @BindView(R.id.auth_et_registerusername) EditText registerUsernameField;
    @BindView(R.id.auth_et_registerpassword) EditText registerPasswordField;
    @BindView(R.id.auth_btn_register) Button registerButton;

    public static final String KEY_ADDRESS = "address";
    public static final String KEY_AUTHTOKEN = "authtoken";

    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        ButterKnife.bind(this);

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);

        Intent data = getIntent();
        handleIntentData(data);
    }

    private void handleIntentData(Intent data) {
        if (data != null) {
            if (data.hasExtra(KEY_ADDRESS)) {
                address = data.getStringExtra(KEY_ADDRESS);
            }
        }
    }

    @OnTextChanged({
            R.id.auth_et_loginusername,
            R.id.auth_et_loginpassword
    })
    public void loginTextFieldsChanged() {
        String username, password;

        username = loginUsernameField.getText().toString();
        password = loginPasswordField.getText().toString();

        boolean shouldEnable = !username.isEmpty() && !password.isEmpty();

        loginButton.setEnabled(shouldEnable);
    }

    @OnClick(R.id.auth_btn_login)
    public void login() {
        String username, password;

        username = loginUsernameField.getText().toString();
        password = loginPasswordField.getText().toString();

        BackgroundLoginTask t = new BackgroundLoginTask(this);
        t.execute();
    }

    private class BackgroundLoginTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;

        public BackgroundLoginTask(AuthActivity activity) {
            progressDialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Logging in...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            String username, password;
            String token;

            username = loginUsernameField.getText().toString();
            password = loginPasswordField.getText().toString();

            // TODO: detect connection errors
            FlackApi api = new FlackApi(address);
            token = api.login(username, password);

            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            loginTaskReturnHandler(token);
        }
    }

    private void loginTaskReturnHandler(String token) {
        if (token != null) {
            Log.d(Constants.APP_NAME, "(ok) token retrieved");

            Intent data = new Intent();

            data.putExtra(KEY_AUTHTOKEN, token);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        else {
            Log.d(Constants.APP_NAME, "(err) invalid credentials provided");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    "Incorrect username or password. Please try again."
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

    @OnTextChanged({
            R.id.auth_et_registerusername,
            R.id.auth_et_registerpassword
    })
    public void registerTextFieldsChanged() {
        String username, password;

        username = registerUsernameField.getText().toString();
        password = registerPasswordField.getText().toString();

        // TODO: check if user available
        boolean shouldEnable = !username.isEmpty() && !password.isEmpty();

        registerButton.setEnabled(shouldEnable);
    }

    @OnClick(R.id.auth_btn_register)
    public void register() {
        String username, password;

        username = registerUsernameField.getText().toString();
        password = registerPasswordField.getText().toString();

        BackgroundLoginTask t = new BackgroundLoginTask(this);
        t.execute();
    }

    private void authTaskReturnHandler(String token) {
        if (token != null) {
            Log.d(Constants.APP_NAME, "good token!");
            // TODO: finish activity, return token
        }
        else {
            Log.d(Constants.APP_NAME, "bad token!");
            // TODO: show error dialog
            // "Incorrect username or password. Please try again."
        }
    }
}
