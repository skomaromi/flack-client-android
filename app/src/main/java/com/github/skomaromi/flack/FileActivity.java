package com.github.skomaromi.flack;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

public class FileActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    @BindView(R.id.file_rv) RecyclerView recyclerView;
    @BindView(R.id.file_srl) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.file_tv_nofiles) TextView noFilesMessage;
    @BindView(R.id.file_el_noconnection) View noConnectionMessage;

    private ArrayList<File> mFileArrayList;
    private FileAdapter mAdapter;
    private SqlHelper mSqlHelper;

    private MessageFile mPendingDownload;

    private WebSocketService mService;
    private FileBroadcastReceiver mBroadcastReceiver;
    private boolean mConnected;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.WebSocketBinder binder = (WebSocketService.WebSocketBinder) service;
            mService = binder.getService();
            setUiConnectionState(mService.isConnected());
        }

        @Override public void onServiceDisconnected(ComponentName name) {}
    };

    private FileClickCallback mDownloadClickCallback = new FileClickCallback() {
        @Override
        public void onClick(File file) {
            MessageFile download = new MessageFile(file.getHash(), file.getName());
            if (!mConnected) {
                showShortToast("No connection.");
            }
            else if (!hasWritePermission()) {
                setPendingDownload(download);
                requestWritePermission();
            }
            else {
                startDownload(download);
            }
        }
    };

    private FileClickCallback mHashClickCallback = new FileClickCallback() {
        @Override
        public void onClick(File file) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(
                    "File IPFS hash",
                    file.getHash()
            );
            clipboardManager.setPrimaryClip(clipData);

            Toast.makeText(
                    FileActivity.this,
                    "IPFS hash copied to clipboard!",
                    Toast.LENGTH_SHORT
                 )
                 .show();
        }
    };

    private FileClickCallback mUrlClickCallback = new FileClickCallback() {
        @Override
        public void onClick(File file) {
            String ipfsUrl = String.format(
                    "https://ipfs.io/ipfs/%s/%s",
                    file.getHash(),
                    file.getName()
            );
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(
                    "File IPFS hash",
                    ipfsUrl
            );
            clipboardManager.setPrimaryClip(clipData);

            Toast.makeText(
                    FileActivity.this,
                    "Public IPFS URL copied to clipboard!",
                    Toast.LENGTH_SHORT
                 )
                 .show();
        }
    };

    private class FileBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int objectType = intent.getIntExtra(WebSocketService.KEY_OBJECTTYPE, -1);

            if (objectType == WebSocketService.TYPE_CONNSTATUSCHANGED) {
                boolean connected = intent.getBooleanExtra(WebSocketService.KEY_CONNSTATUS, false);
                setUiConnectionState(connected);
            }
        }
    }

    private class BackgroundFileFetchTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            FlackApi api = new FlackApi(FileActivity.this);
            ArrayList<File> files = api.getFiles();
            mSqlHelper.addFiles(files);
            mFileArrayList.clear();
            mFileArrayList.addAll(files);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
            fileNumberChanged();
            swipeRefreshLayout.setRefreshing(false);
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
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(FileActivity.this);
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
            flushPendingDownload();

            if (url != null) {
                try {
                    Toast.makeText(
                            FileActivity.this,
                            "Downloading file...",
                            Toast.LENGTH_SHORT
                         )
                         .show();

                    Uri uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    DownloadManager manager = (DownloadManager) FileActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
                    // enqueue() NPE handled by a catch-all catch block below
                    manager.enqueue(request);
                }
                catch (Exception e) {
                    Toast.makeText(
                            FileActivity.this,
                            "An error occurred while downloading.",
                            Toast.LENGTH_SHORT
                         )
                         .show();
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(
                        FileActivity.this,
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQCODE_PERMISSION_WRITE:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        resumePendingDownload();
                    }
                    else {
                        flushPendingDownload();
                        showShortToast("Storage write permission not granted.");
                    }
                }
        }
    }

    private void startDownload(MessageFile file) {
        BackgroundDownloadTask t = new BackgroundDownloadTask(
                file.getHash(),
                file.getName()
        );
        t.execute();
    }

    private void setPendingDownload(MessageFile file) {
        mPendingDownload = file;
    }

    private void resumePendingDownload() {
        if (mPendingDownload != null) {
            startDownload(mPendingDownload);
        }
    }

    private void flushPendingDownload() {
        if (mPendingDownload != null) {
            mPendingDownload = null;
        }
    }

    private void requestWritePermission() {
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Constants.REQCODE_PERMISSION_WRITE);
    }

    private void requestPermission(String permission, int requestCode) {
        String[] permissions = new String[] { permission };
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    private boolean hasWritePermission() {
        return hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private boolean hasPermission(String permission) {
        if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    private void showShortToast(String text) {
        showToast(text, true);
    }

    private void showToast(String text, boolean isShort) {
        Toast.makeText(
                this,
                text,
                isShort? Toast.LENGTH_SHORT : Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        ButterKnife.bind(this);
        swipeRefreshLayout.setOnRefreshListener(this);

        mSqlHelper = new SqlHelper(this);
        setUpRecyclerView();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpBroadcastReceiver();

        setUiConnectionState(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindWebSocketsService();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    public void onRefresh() {
        BackgroundFileFetchTask t = new BackgroundFileFetchTask();
        t.execute();
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
        mBroadcastReceiver = new FileBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.APP_PKG_NAME);

        registerReceiver(mBroadcastReceiver, filter);
    }

    private void setUpRecyclerView() {
        mFileArrayList = mSqlHelper.getFiles();

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new FileAdapter(mFileArrayList, mDownloadClickCallback, mHashClickCallback, mUrlClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);

        fileNumberChanged();
    }

    private void fileNumberChanged() {
        if (mFileArrayList.isEmpty()) {
            noFilesMessage.setVisibility(View.VISIBLE);
        }
        else {
            noFilesMessage.setVisibility(View.GONE);
        }
    }

    private void bindWebSocketsService() {
        Intent webSocketsService = new Intent(this, WebSocketService.class);
        bindService(webSocketsService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void setUiConnectionState(boolean connected) {
        mConnected = connected;

        swipeRefreshLayout.setEnabled(connected);

        boolean shouldShowNoConnectionMsg = !connected;
        setNoConnectionMessageVisible(shouldShowNoConnectionMsg);
    }

    private void setNoConnectionMessageVisible(boolean visible) {
        if (visible) {
            noConnectionMessage.setVisibility(View.VISIBLE);
        }
        else {
            noConnectionMessage.setVisibility(View.GONE);
        }
    }
}
