package com.github.skomaromi.flack;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RoomActivity extends AppCompatActivity {
    @BindView(R.id.room_rv) RecyclerView recyclerView;
    @BindView(R.id.room_tv_norooms) TextView noRoomsMessage;
    @BindView(R.id.room_fab_addroom) FloatingActionButton addRoomButton;
    @BindView(R.id.room_el_noconnection) NoConnectionMessage noConnectionMessage;

    private WebSocketService mService;

    private ArrayList<Room> mRoomArrayList;
    private RoomAdapter mAdapter;
    private SqlHelper mSqlHelper;
    private RoomBroadcastReceiver mBroadcastReceiver;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.WebSocketBinder binder = (WebSocketService.WebSocketBinder) service;
            mService = binder.getService();
            setUiConnectionState(mService.isConnected());
        }

        @Override public void onServiceDisconnected(ComponentName name) {}
    };

    private RoomClickCallback mOnRoomClickCallback = new RoomClickCallback() {
        @Override
        public void onClick(Room room) {
            Intent messagesActivity = new Intent(
                    RoomActivity.this, MessageActivity.class
            );
            messagesActivity.putExtra(MessageActivity.KEY_ROOMID, room.getServerId());
            startActivity(messagesActivity);
        }
    };

    private class RoomBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int objectType = intent.getIntExtra(WebSocketService.KEY_OBJECTTYPE, -1);

            if (objectType == WebSocketService.TYPE_ROOM) {
                addRoom(
                        intent.getIntExtra(WebSocketService.KEY_ROOM_SERVERID, -1),
                        intent.getStringExtra(WebSocketService.KEY_ROOM_NAME),
                        intent.getLongExtra(WebSocketService.KEY_ROOM_TIMECREATED, -1)
                );
            }
            else if (objectType == WebSocketService.TYPE_MESSAGELITE) {
                addMessage(
                        intent.getIntExtra(WebSocketService.KEY_MESSAGELITE_ROOMID, -1),
                        intent.getStringExtra(WebSocketService.KEY_MESSAGELITE_TEXT),
                        intent.getLongExtra(WebSocketService.KEY_MESSAGELITE_TIMECREATED, -1)
                );
            }
            else if (objectType == WebSocketService.TYPE_CONNSTATUSCHANGED) {
                boolean connected = intent.getBooleanExtra(WebSocketService.KEY_CONNSTATUS, false);
                setUiConnectionState(connected);
            }
        }
    }

    @OnClick(R.id.room_fab_addroom)
    public void addRoomButtonClicked() {
        startRoomCreateActivity();
    }

    private void startRoomCreateActivity() {
        Intent roomCreateActivity = new Intent(this, RoomCreateActivity.class);
        startActivityForResult(roomCreateActivity, Constants.REQCODE_ACTIVITY_ROOMCREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQCODE_ACTIVITY_ROOMCREATE && resultCode == Activity.RESULT_OK) {
            String name = data.getStringExtra(RoomCreateActivity.KEY_NAME);
            int[] participants = data.getIntArrayExtra(RoomCreateActivity.KEY_PARTICIPANTS);

            mService.sendRoom(
                    name,
                    participants
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        ButterKnife.bind(this);

        mSqlHelper = new SqlHelper(this);
        setUpRecyclerView();
        setUpBroadcastReceiver();
        startWebSocketsService();

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
        FlackApplication.setCurrentRoom(FlackApplication.ON_ROOM_LIST);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_room_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.room_mi_files:
                startFileActivity();
                return true;
            case R.id.room_mi_server:
                startServerInputActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpBroadcastReceiver() {
        mBroadcastReceiver = new RoomBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.APP_PKG_NAME);

        registerReceiver(mBroadcastReceiver, filter);
    }

    private void startServerInputActivity() {
        Intent serverInputActivity = new Intent(this, ServerInputActivity.class);
        serverInputActivity.putExtra(ServerInputActivity.KEY_FROM_STARTACTIVITY, false);
        startActivity(serverInputActivity);
    }

    private void startFileActivity() {
        Intent fileActivity = new Intent(this, FileActivity.class);
        startActivity(fileActivity);
    }

    private void setUpRecyclerView() {
        mRoomArrayList = mSqlHelper.getRooms();

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new RoomAdapter(mRoomArrayList, mOnRoomClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);

        roomNumberChanged();
    }

    private void addRoom(int id, String name, long created) {
        mRoomArrayList.add(0, new Room(id, name, created));
        mAdapter.notifyDataSetChanged();

        roomNumberChanged();
    }

    private void roomNumberChanged() {
        if (mRoomArrayList.isEmpty()) {
            noRoomsMessage.setVisibility(View.VISIBLE);
        }
        else {
            noRoomsMessage.setVisibility(View.GONE);
        }
    }

    private void addMessage(int roomId, String messageText, long messageTime) {
        for (int i = 0; i < mRoomArrayList.size(); i++) {
            Room room = mRoomArrayList.get(i);
            if (room.getServerId() == roomId) {
                if (room.getTimeModified() < messageTime) {
                    room.setLastMessageText(messageText);
                    room.setTimeLastMessage(messageTime);
                    room.setTimeModified(messageTime);
                    mRoomArrayList.set(i, room);
                    Collections.sort(mRoomArrayList, Collections.reverseOrder());
                    mAdapter.notifyDataSetChanged();
                }

                break;
            }
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

    private void setUiConnectionState(boolean connected) {
        setAddRoomButtonEnabled(connected);

        boolean shouldShowNoConnectionMsg = !connected;
        setNoConnectionMessageVisible(shouldShowNoConnectionMsg);
    }

    private void setAddRoomButtonEnabled(boolean enabled) {
        addRoomButton.setEnabled(enabled);

        if (enabled) {
            addRoomButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.fabEnabledBg)));
            addRoomButton.clearColorFilter();
        }
        else {
            addRoomButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.fabDisabledBg)));
            addRoomButton.setColorFilter(ContextCompat.getColor(this, R.color.fabDisabledFg));
        }

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
