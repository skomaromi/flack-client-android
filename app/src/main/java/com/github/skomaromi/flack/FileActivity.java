package com.github.skomaromi.flack;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
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

    private ArrayList<File> mFileArrayList;
    private FileAdapter mAdapter;
    private SqlHelper mSqlHelper;

    private FileClickCallback mDownloadClickCallback = new FileClickCallback() {
        @Override
        public void onClick(File file) {
            BackgroundDownloadTask t = new BackgroundDownloadTask(
                    file.getHash(),
                    file.getName()
            );
            t.execute();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        ButterKnife.bind(this);
        swipeRefreshLayout.setOnRefreshListener(this);

        mSqlHelper = new SqlHelper(this);
        setUpRecyclerView();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
}
