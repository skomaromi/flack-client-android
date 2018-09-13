package com.github.skomaromi.flack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {
    @BindView(R.id.message_rv)
    RecyclerView recyclerView;
    @BindView(R.id.message_tv_nomessages)
    TextView noMessagesText;

    @BindView(R.id.message_et_text)
    EditText messageTextField;
    @BindView(R.id.message_btn_send)
    ImageButton sendButton;
    @BindView(R.id.message_btn_addlocation)
    ImageButton locationButton;
    @BindView(R.id.message_btn_addfile)
    ImageButton fileButton;

    public static final String KEY_ROOMID = "room_id";

    private int roomId;
    private WebSocketService mService;

    private ArrayList<Message> mMessageArrayList;
    private MessageAdapter mAdapter;
    private SqlHelper mSqlHelper;
    private MessageBroadcastReceiver mBroadcastReceiver;

    private ProgressDialog mProgressDialog;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private String mLocationProvider;

    // currently written message data
    private Location mMessageLocation;
    private int mMessageFileId;
    private String mMessageFilePath;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.WebSocketBinder binder = (WebSocketService.WebSocketBinder) service;
            mService = binder.getService();
        }

        @Override public void onServiceDisconnected(ComponentName name) {}
    };

    private MessageClickCallback mOnFileClickCallback = new MessageClickCallback() {
        @Override
        public void onClick(Message message) {
            MessageFile file = message.getFile();
            BackgroundDownloadTask t = new BackgroundDownloadTask(
                    file.getHash(),
                    file.getName()
            );
            t.execute();
        }
    };

    private MessageClickCallback mOnLocationClickCallback = new MessageClickCallback() {
        @Override
        public void onClick(Message message) {
            Location location = message.getLocation();
            Uri intentUri = Uri.parse(
                    String.format(
                            Locale.ENGLISH,
                            "geo:0,0?q=%.4f,%.4f(Message+location)",
                            location.getLatitude(),
                            location.getLongitude()
                    )
            );
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, intentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(
                        MessageActivity.this,
                        "Google Maps not installed.",
                        Toast.LENGTH_SHORT
                     )
                     .show();
            }
        }
    };

    private class MessageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int objectType = intent.getIntExtra(WebSocketService.KEY_OBJECTTYPE, -1);
            int messageRoomId = intent.getIntExtra(WebSocketService.KEY_MESSAGE_ROOMID, -1);

            if (objectType == WebSocketService.TYPE_MESSAGE && roomId == messageRoomId) {
                Location location = null;
                if (!intent.getBooleanExtra(WebSocketService.KEY_MESSAGE_LOCATION_ISNULL, true)) {
                    location = new Location(
                            intent.getFloatExtra(WebSocketService.KEY_MESSAGE_LOCATION_LATITUDE, -1),
                            intent.getFloatExtra(WebSocketService.KEY_MESSAGE_LOCATION_LONGITUDE, -1)
                    );
                }

                MessageFile file = null;
                if (!intent.getBooleanExtra(WebSocketService.KEY_MESSAGE_FILE_ISNULL, true)) {
                    file = new MessageFile(
                            intent.getStringExtra(WebSocketService.KEY_MESSAGE_FILE_HASH),
                            intent.getStringExtra(WebSocketService.KEY_MESSAGE_FILE_NAME)
                    );
                }

                addMessage(
                        intent.getStringExtra(WebSocketService.KEY_MESSAGE_SENDER),
                        intent.getStringExtra(WebSocketService.KEY_MESSAGE_CONTENT),
                        intent.getLongExtra(WebSocketService.KEY_MESSAGE_TIMECREATED, -1),
                        location,
                        file
                );
            }
        }
    }

    private class BackgroundDownloadTask extends AsyncTask<Void, Void, String> {
        private String fileHash, fileName;

        public BackgroundDownloadTask(String fileHash, String fileName) {
            this.fileHash = fileHash;
            this.fileName = fileName;
        }

        @Override
        protected String doInBackground(Void... args) {
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(MessageActivity.this);
            String serverAddr = prefs.getString(SharedPreferencesHelper.KEY_SERVERADDR);
            String localUrl = String.format(
                    Locale.ENGLISH,
                    "%s://%s:%d/ipfs/%s/%s",
                    Constants.SERVER_PROTO,
                    serverAddr,
                    Constants.SERVER_IPFS_PORT,
                    fileHash,
                    fileName
            );
            if (testUrl(localUrl)) {
                return localUrl;
            }

            String globalUrl = String.format("https://ipfs.io/ipfs/%s/%s", fileHash, fileName);
            if (testUrl(globalUrl)) {
                return globalUrl;
            }

            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
                try {
                    Toast.makeText(
                            MessageActivity.this,
                            "Downloading file...",
                            Toast.LENGTH_SHORT
                    )
                            .show();

                    Uri uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    DownloadManager manager = (DownloadManager) MessageActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
                    // enqueue() NPE handled by a catch-all catch block below
                    manager.enqueue(request);
                } catch (Exception e) {
                    Toast.makeText(
                            MessageActivity.this,
                            "An error occured while downloading.",
                            Toast.LENGTH_SHORT
                    )
                            .show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(
                        MessageActivity.this,
                        "Cannot download file.",
                        Toast.LENGTH_SHORT
                )
                        .show();
            }
        }

        private boolean testUrl(String url) {
            OkHttpClient client = new OkHttpClient.Builder()
                                          .connectTimeout(2, TimeUnit.SECONDS)
                                          .writeTimeout(2, TimeUnit.SECONDS)
                                          .readTimeout(2, TimeUnit.SECONDS)
                                          .build();

            Request request = new Request.Builder()
                                      .url(url)
                                      .head()
                                      .build();

            Response response;
            try {
                response = client.newCall(request).execute();
                return response.code() == 200;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @OnTextChanged(R.id.message_et_text)
    public void messageTextChanged() {
        String messageText;

        messageText = messageTextField.getText().toString();

        boolean shouldEnable = !messageText.isEmpty();

        sendButton.setEnabled(shouldEnable);
    }

    @OnClick(R.id.message_btn_addlocation)
    public void locationButtonClicked() {
        if (mMessageLocation != null) {
            mMessageLocation = null;
            setLocationButtonState(false);
            Toast.makeText(
                    MessageActivity.this,
                    "Location removed!",
                    Toast.LENGTH_SHORT
                 )
                 .show();
            return;
        }

        if (!isLocationTurnedOn()) {
            Toast.makeText(
                    MessageActivity.this,
                    "Location not turned on.",
                    Toast.LENGTH_SHORT
                 )
                 .show();
            return;
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Acquiring location...");
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                stopListeningForLocation();
            }
        });
        mProgressDialog.show();
        startListeningForLocation();
    }

    private boolean isLocationTurnedOn() {
        int locationMode;

        try {
            locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        }
        catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    private class MessageLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(android.location.Location location) {
            if (location.getAccuracy() < 1000.0) {
                locationFound(new Location(
                        (float)location.getLatitude(),
                        (float)location.getLongitude()
                ));
            }
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    }

    private void startListeningForLocation() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        if (mLocationListener == null) {
            mLocationListener = new MessageLocationListener();
        }

        Criteria criteria = new Criteria();
        mLocationProvider = mLocationManager.getBestProvider(criteria, true);

        if (!hasLocationPermission()) {
            requestPermission();
        }
        else {
            requestUserLocationUpdates();
        }
    }

    private void requestUserLocationUpdates() {
        // permissions handled by preceding hasLocationPermission() and requestPermission() methods
        mLocationManager.requestLocationUpdates(
                mLocationProvider,
                1000,
                100,
                mLocationListener
        );
    }

    private boolean hasLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    private void requestPermission() {
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        ActivityCompat.requestPermissions(this, permissions, Constants.REQCODE_PERMISSION_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQCODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startListeningForLocation();
                    }
                    else {
                        stopListeningForLocation();
                    }
                }
        }
    }

    private void locationFound(Location location) {
        mMessageLocation = location;
        stopListeningForLocation();
        setLocationButtonState(true);

        Toast.makeText(
                MessageActivity.this,
                "Location found!",
                Toast.LENGTH_SHORT)
                .show();
    }

    private void stopListeningForLocation() {
        Log.d(Constants.APP_NAME, "MessageActivity: location listening stopped!");
        mLocationManager.removeUpdates(mLocationListener);
        mProgressDialog.hide();
    }

    private void setLocationButtonState(boolean locationStored) {
        if (locationStored) {
            locationButton.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        }
        else {
            locationButton.clearColorFilter();
        }
    }

    @OnClick(R.id.message_btn_addfile)
    public void fileButtonClicked() {
        boolean didRemoveFile = false;
        if (mMessageFileId != -1) {
            // file id set by internal FilePicker activity
            mMessageFileId = -1;
            didRemoveFile = true;

        }
        if (mMessageFilePath != null) {
            // file Uri set by Android's file picker dialog
            mMessageFilePath = null;
            didRemoveFile = true;
        }
        if (didRemoveFile) {
            setFileButtonState(false);
            Toast.makeText(
                    MessageActivity.this,
                    "File removed from message.",
                    Toast.LENGTH_SHORT
                 )
                 .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add file");
        builder.setMessage("Do you want to share an existing file or upload a new one?");
        builder.setNegativeButton("Cancel", null);
        builder.setNeutralButton("Pick existing", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startServerFilePickerActivity();
            }
        });
        builder.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startLocalFilePickerActivity();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startServerFilePickerActivity() {
        Intent serverPickerActivity = new Intent(this, FilePickActivity.class);
        startActivityForResult(serverPickerActivity, Constants.REQCODE_ACTIVITY_FILEPICKER);
    }

    private void startLocalFilePickerActivity() {
        Intent pickerActivity = new Intent(this, FilePickerActivity.class);
        // statement below is necessary to prevent MaterialFilePicker from NPE'ing
        pickerActivity.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
        startActivityForResult(pickerActivity, Constants.REQCODE_ACTIVITY_ANDROID_FILEPICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQCODE_ACTIVITY_FILEPICKER && resultCode == Activity.RESULT_OK) {
            mMessageFileId = data.getIntExtra(FilePickActivity.KEY_FILE_SERVERID, -1);
            setFileButtonState(true);
        }
        else if (requestCode == Constants.REQCODE_ACTIVITY_ANDROID_FILEPICKER && resultCode == Activity.RESULT_OK) {
            mMessageFilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            setFileButtonState(true);
        }
    }

    private void setFileButtonState(boolean fileStored) {
        if (fileStored) {
            fileButton.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        }
        else {
            fileButton.clearColorFilter();
        }
    }

    @OnClick(R.id.message_btn_send)
    public void sendButtonClicked() {
        trySendMessage();
    }

    private void trySendMessage() {
        if (mMessageFilePath != null) {
            BackgroundUploadTask t = new BackgroundUploadTask();
            t.execute(mMessageFilePath);
        }
        else {
            String content = messageTextField.getText().toString();

            mService.sendMessage(
                    content,
                    mMessageFileId,
                    roomId,
                    mMessageLocation
            );

            resetMessageInput();
        }
    }

    private void resetMessageInput() {
        // reset text
        messageTextField.setText("");

        // reset location
        mMessageLocation = null;
        setLocationButtonState(false);

        // reset file
        mMessageFileId = -1;
        mMessageFilePath = null;
        setFileButtonState(false);

        // re-disable send button
        sendButton.setEnabled(false);
    }

    private class BackgroundUploadTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(MessageActivity.this);
            mProgressDialog.setMessage("Uploading...");
            mProgressDialog.show();
        }

        @Override
        protected Integer doInBackground(String... paths) {
            String filePath;
            if (paths.length == 0) {
                return -1;
            }
            else {
                filePath = paths[0];
            }

            FlackApi api = new FlackApi(MessageActivity.this);
            int fileId = api.uploadFile(filePath, MessageActivity.this);
            return fileId;
        }

        @Override
        protected void onPostExecute(Integer fileId) {
            if (fileId != -1) {
                mMessageFileId = fileId;
                mMessageFilePath = null;
                mProgressDialog.hide();
                trySendMessage();
            }
            else {
                mProgressDialog.hide();
                Toast.makeText(
                        MessageActivity.this,
                        "File upload failed.",
                        Toast.LENGTH_SHORT
                     )
                     .show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        ButterKnife.bind(this);

        Intent data = getIntent();
        handleIntentData(data);

        mSqlHelper = new SqlHelper(this);
        setUpTitle();
        setUpRecyclerView();
        resetMessageInput();
        setUpBroadcastReceiver();
        startWebSocketsService();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(Constants.APP_NAME, String.format("MessageActivity started with roomId '%d'!", roomId));
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindWebSocketsService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FlackApplication.setCurrentRoom(roomId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FlackApplication.setCurrentRoom(FlackApplication.NO_ACTIVITY_VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        scrollToBottom();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!messageTextField.getText().toString().isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Discard message?");
                    builder.setMessage("Your message will be lost if you close room without sending.");
                    builder.setNegativeButton("Cancel", null);
                    builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!messageTextField.getText().toString().isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Discard message?");
            builder.setMessage("Your message will be lost if you close room without sending.");
            builder.setNegativeButton("Cancel", null);
            builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else super.onBackPressed();
    }


    private void setUpBroadcastReceiver() {
        mBroadcastReceiver = new MessageBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.APP_PKG_NAME);

        registerReceiver(mBroadcastReceiver, filter);
    }

    private void handleIntentData(Intent data) {
        if (data != null) {
            if (data.hasExtra(KEY_ROOMID)) {
                roomId = data.getIntExtra(KEY_ROOMID, -1);
            }
        }
    }

    private void setUpTitle() {
        Room currentRoom = mSqlHelper.getRoom(roomId);

        if (currentRoom != null) {
            setTitle(currentRoom.getName());
        }
    }

    private void setUpRecyclerView() {
        mMessageArrayList = mSqlHelper.getMessages(roomId);

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new MessageAdapter(mMessageArrayList, mOnFileClickCallback, mOnLocationClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);

        messageNumberChanged();
    }

    private void addMessage(String sender, String content, long timeCreated, Location location, MessageFile file) {
        mMessageArrayList.add(new Message(sender, content, timeCreated, location, file));
        mAdapter.notifyDataSetChanged();

        messageNumberChanged();
    }

    private void messageNumberChanged() {
        if (mMessageArrayList.isEmpty()) {
            noMessagesText.setVisibility(View.VISIBLE);
        }
        else {
            noMessagesText.setVisibility(View.GONE);
            scrollToBottom();
        }
    }

    private void scrollToBottom() {
        recyclerView.scrollToPosition(mMessageArrayList.size() - 1);
    }

    private void startWebSocketsService() {
        Intent webSocketsService = new Intent(this, WebSocketService.class);
        startService(webSocketsService);
    }

    private void bindWebSocketsService() {
        Intent webSocketsService = new Intent(this, WebSocketService.class);
        bindService(webSocketsService, mConnection, Context.BIND_AUTO_CREATE);
    }
}
