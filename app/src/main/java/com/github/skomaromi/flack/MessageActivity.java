package com.github.skomaromi.flack;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {
    @BindView(R.id.message_rv) RecyclerView recyclerView;
    @BindView(R.id.message_tv_nomessages) TextView noMessagesText;

    public static final String KEY_ROOMID = "room_id";

    private WebSocketService mService;
    private int roomId;

    private ArrayList<Message> mMessageArrayList;
    private MessageAdapter mAdapter;
    private SqlHelper mSqlHelper;
    private MessageBroadcastReceiver mBroadcastReceiver;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.LocalBinder binder = (WebSocketService.LocalBinder) service;
            mService = binder.getService();
            mService.setCurrentRoom(roomId);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private MessageClickCallback mOnFileClickCallback = new MessageClickCallback() {
        @Override
        public void onClick(Message message) {
            // TODO: do something here, like downloading.
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
            // TODO: do something here, like opening Maps.
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
            }
            else {
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
                }
                catch (Exception e) {
                    Toast.makeText(
                            MessageActivity.this,
                            "An error occured while downloading.",
                            Toast.LENGTH_SHORT
                         )
                         .show();
                    e.printStackTrace();
                }
            }
            else {
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
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
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
        setUpBroadcastReceiver();
        startWebSocketsService();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(Constants.APP_NAME, String.format("MessageActivity started with roomId '%d'!", roomId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindWebSocketsService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mService != null) {
            mService.setCurrentRoom(WebSocketService.CR_NONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mConnection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        }
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
