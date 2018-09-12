package com.github.skomaromi.flack;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilePickActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    @BindView(R.id.filepick_rv) RecyclerView recyclerView;
    @BindView(R.id.filepick_srl) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.filepick_tv_nofiles) TextView noFilesMessage;

    public static final String KEY_FILE_SERVERID = "file_server_id";

    private ArrayList<File> mFileArrayList;
    private FilePickAdapter mAdapter;
    private SqlHelper mSqlHelper;

    private FileClickCallback mCallback = new FileClickCallback() {
        @Override
        public void onClick(File file) {
            int fileId = file.getServerId();

            Intent data = new Intent();
            data.putExtra(KEY_FILE_SERVERID, fileId);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    };

    private class BackgroundFileFetchTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            FlackApi api = new FlackApi(FilePickActivity.this);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_pick);

        ButterKnife.bind(this);
        swipeRefreshLayout.setOnRefreshListener(this);

        mSqlHelper = new SqlHelper(this);
        setUpRecyclerView();

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.icon_close_fff);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        mAdapter = new FilePickAdapter(mFileArrayList, mCallback);

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
