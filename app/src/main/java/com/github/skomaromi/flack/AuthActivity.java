package com.github.skomaromi.flack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class AuthActivity extends AppCompatActivity {
    @BindView(R.id.auth_tie_loginusername)
    TextInputEditText loginUsernameField;
    @BindView(R.id.auth_tie_loginpassword)
    TextInputEditText loginPasswordField;
    @BindView(R.id.auth_btn_login)
    Button loginButton;

    @BindView(R.id.auth_til_registerusername)
    TextInputLayout registerUsernameFieldContainer;
    @BindView(R.id.auth_tie_registerusername)
    TextInputEditText registerUsernameField;

    @BindView(R.id.auth_til_registerpassword)
    TextInputLayout registerPasswordFieldContainer;
    @BindView(R.id.auth_tie_registerpassword)
    TextInputEditText registerPasswordField;

    @BindView(R.id.auth_btn_register)
    Button registerButton;

    public static final String KEY_ADDRESS = "address";
    public static final String KEY_AUTHTOKEN = "authtoken";

    private final String usernameRegex = "^[a-z0-9\\.\\-]*$";

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
            R.id.auth_tie_loginusername,
            R.id.auth_tie_loginpassword
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
        BackgroundLoginTask t = new BackgroundLoginTask(this);
        t.execute();
    }

    private class BackgroundLoginTask extends AsyncTask<Void, Void, String> {
        private AuthActivity activity;
        private ProgressDialog progressDialog;

        public BackgroundLoginTask(AuthActivity activity) {
            progressDialog = new ProgressDialog(activity);
            this.activity = activity;
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
            token = api.login(username, password, activity);

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
            R.id.auth_tie_registerusername,
            R.id.auth_tie_registerpassword
    })
    public void registerTextFieldsChanged() {
        String username, password;

        username = registerUsernameField.getText().toString();
        password = registerPasswordField.getText().toString();

        boolean usernameOk =
                !username.isEmpty() &&
                username.matches(usernameRegex);

        boolean passwordOk =
                !password.isEmpty() &&
                password.length() >= 6;

        // TODO: check if user available
        boolean shouldEnable = usernameOk && passwordOk;

        registerButton.setEnabled(shouldEnable);
    }

    @OnTextChanged(R.id.auth_tie_registerusername)
    public void registerUsernameFieldChanged() {
        String username = registerUsernameField.getText().toString();

        if (!username.matches(usernameRegex)) {
            registerUsernameFieldContainer.setError(
                    "Allowed characters are lowercase letters, digits, dots " +
                            "and hyphens."
            );
        }
        else {
            registerUsernameFieldContainer.setError(null);
            // setErrorEnabled(false) hides the space created by a potential
            // previous setError(<non-null String>) call
            registerUsernameFieldContainer.setErrorEnabled(false);
        }
    }

    @OnTextChanged(R.id.auth_tie_registerpassword)
    public void registerPasswordFieldChanged() {
        String password = registerPasswordField.getText().toString();

        if (password.length() < 6) {
            registerPasswordFieldContainer.setError(
                    "Password must be at least 6 characters long."
            );
        }
        else {
            registerPasswordFieldContainer.setError(null);
            registerPasswordFieldContainer.setErrorEnabled(false);
        }
    }

    @OnClick(R.id.auth_btn_register)
    public void register() {
        BackgroundRegisterTask t = new BackgroundRegisterTask(this);
        t.execute();
    }

    private class BackgroundRegisterTask extends AsyncTask<Void, Void, String> {
        private AuthActivity activity;
        private ProgressDialog progressDialog;

        public BackgroundRegisterTask(AuthActivity activity) {
            progressDialog = new ProgressDialog(activity);
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Registering...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            // TODO: use exceptions to detect error type
            String username, password;
            String token;

            username = registerUsernameField.getText().toString();
            password = registerPasswordField.getText().toString();

            FlackApi api = new FlackApi(address);
            token = api.register(username, password, activity);

            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            registerTaskReturnHandler(token);
        }
    }

    private void registerTaskReturnHandler(String token) {
        if (token != null) {
            Log.d(
                    Constants.APP_NAME,
                    "(ok) registration successful, token retrieved"
            );

            Intent data = new Intent();

            data.putExtra(KEY_AUTHTOKEN, token);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        else {
            Log.d(
                    Constants.APP_NAME,
                    "(err) problems occurred while registering"
            );

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    "Username not available. Please try specifying a " +
                            "different one."
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
