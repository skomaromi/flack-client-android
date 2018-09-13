package com.github.skomaromi.flack;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;

public class RoomCreateActivity extends AppCompatActivity {
    @BindView(R.id.roomcreate_rv) RecyclerView recyclerView;
    
    @BindView(R.id.roomcreate_til_roomname) TextInputLayout roomNameFieldContainer;
    @BindView(R.id.roomcreate_tiet_roomname) TextInputEditText roomNameField;
    
    @BindView(R.id.roomcreate_tv_userslabel) TextView userListLabel;
    @BindView(R.id.roomcreate_tv_nousers) TextView noUsersMessage;

    @BindView(R.id.roomcreate_el_noconnection) View noConnectionMessage;

    public static final String KEY_NAME = "room_name";
    public static final String KEY_PARTICIPANTS = "room_participants";

    private ArrayList<User> mUserArrayList;
    private SparseIntArray mRoomMembers;
    private RoomCreateAdapter mAdapter;
    private ProgressDialog mProgressDialog;
    private MenuItem mCreateButton;

    private WebSocketService mService;
    private RoomCreateBroadcastReceiver mBroadcastReceiver;
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

    private UserClickCallback mUserClickCallback = new UserClickCallback() {
        @Override
        public void onClick(User user) {
            int userServerId = user.getServerId();

            if (mRoomMembers.get(userServerId, -1) != -1) {
                int keyValuePairIdx = mRoomMembers.indexOfKey(userServerId);
                mRoomMembers.removeAt(keyValuePairIdx);
            }
            else {
                mRoomMembers.append(userServerId, userServerId);
            }

            validateInputs();
        }
    };

    private class RoomCreateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int objectType = intent.getIntExtra(WebSocketService.KEY_OBJECTTYPE, -1);

            if (objectType == WebSocketService.TYPE_CONNSTATUSCHANGED) {
                boolean connected = intent.getBooleanExtra(WebSocketService.KEY_CONNSTATUS, false);
                setUiConnectionState(connected);
            }
        }
    }

    private class BackgroundUserFetchTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(RoomCreateActivity.this);
            mProgressDialog.setMessage("Fetching users...");
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FlackApi api = new FlackApi(RoomCreateActivity.this);
            ArrayList<User> users = api.getUsers();
            mUserArrayList.clear();
            mUserArrayList.addAll(users);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProgressDialog.dismiss();
            mAdapter.notifyDataSetChanged();
            userNumberChanged();
        }
    }
    
    @OnTextChanged(R.id.roomcreate_tiet_roomname)
    public void roomNameFieldChanged() {
        boolean isRoomNameValid = isRoomNameValid();
        
        if (!isRoomNameValid) {
            roomNameFieldContainer.setError("Room name should contain at least one character and up to 255 characters.");
        }
        else {
            roomNameFieldContainer.setError(null);
            roomNameFieldContainer.setErrorEnabled(false);
        }
        
        boolean shouldEnable = isRoomNameValid && isRoomMemberCountOk();
        
        mCreateButton.setEnabled(shouldEnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_create);

        ButterKnife.bind(this);

        mRoomMembers = new SparseIntArray();
        setUpRecyclerView();
        fetchUsers();

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.icon_close_fff);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_room_create_menu, menu);

        mCreateButton = menu.findItem(R.id.roomcreate_mi_create);
        mCreateButton.setEnabled(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.roomcreate_mi_create:
                Intent data = new Intent();

                String roomName = roomNameField.getText().toString();
                int[] roomParticipants = getParticipantsIntArray();

                data.putExtra(KEY_NAME, roomName);
                data.putExtra(KEY_PARTICIPANTS, roomParticipants);
                setResult(Activity.RESULT_OK, data);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpBroadcastReceiver() {
        mBroadcastReceiver = new RoomCreateBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.APP_PKG_NAME);

        registerReceiver(mBroadcastReceiver, filter);
    }

    private void setUpRecyclerView() {
        mUserArrayList = new ArrayList<>();

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new RoomCreateAdapter(mUserArrayList, mUserClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);

        userNumberChanged();
    }

    private void userNumberChanged() {
        if (mUserArrayList.isEmpty()) {
            userListLabel.setVisibility(View.GONE);
            noUsersMessage.setVisibility(View.VISIBLE);
        }
        else {
            userListLabel.setVisibility(View.VISIBLE);
            noUsersMessage.setVisibility(View.GONE);
        }
    }

    private void validateInputs() {
        if (isRoomMemberCountOk() && isRoomNameValid() && mConnected) {
            mCreateButton.setEnabled(true);
        }
        else {
            mCreateButton.setEnabled(false);
        }
    }

    private boolean isRoomNameValid() {
        String roomName = roomNameField.getText().toString();

        return !roomName.isEmpty() && roomName.length() < 256;
    }

    private boolean isRoomMemberCountOk() {
        /*
         * room member count is valid if mRoomMembers contains at least one
         * element. room creator is added on save, causing the room to have at
         * least two participants, which is the minimal sane number for a
         * proper discussion.
         */
        return mRoomMembers.size() > 0;
    }

    private int[] getParticipantsIntArray() {
        // +1 to leave space for room creator added in WebSocketService.sendRoom()
        int[] participants = new int[mRoomMembers.size() + 1];

        for (int i = 0; i < mRoomMembers.size(); i++) {
            participants[i] = mRoomMembers.valueAt(i);
        }

        return participants;
    }

    private void fetchUsers() {
        BackgroundUserFetchTask t = new BackgroundUserFetchTask();
        t.execute();
    }

    private void bindWebSocketsService() {
        Intent webSocketsService = new Intent(this, WebSocketService.class);
        bindService(webSocketsService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void setUiConnectionState(boolean connected) {
        mConnected = connected;

        // this might happen before mCreateButton is instantiated, so check if
        // mCreateButton is not null beforehand
        if (mCreateButton != null) {
            if (!connected) {
                // don't bother validating if not connected
                mCreateButton.setEnabled(false);
            } else {
                validateInputs();
            }
        }

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
