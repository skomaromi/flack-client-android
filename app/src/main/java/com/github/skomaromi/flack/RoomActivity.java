package com.github.skomaromi.flack;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RoomActivity extends AppCompatActivity {
    @BindView(R.id.room_rv) RecyclerView recyclerView;

    public static final String KEY_ADDRESS = "address";
    public static final String KEY_AUTHTOKEN = "authtoken";

    private String address, token;

    private ArrayList<Room> mRoomArrayList;
    private RoomAdapter mAdapter;
    private WebSocketService mService;
    private SqlHelper mSqlHelper;
    private RoomBroadcastReceiver mBroadcastReceiver;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.LocalBinder binder = (WebSocketService.LocalBinder) service;
            mService = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
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
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        ButterKnife.bind(this);

        Intent data = getIntent();
        handleIntentData(data);

        mSqlHelper = new SqlHelper(this);
        setUpRecyclerView();
        setUpBroadcastReceiver();
        startWebSocketsService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent webSocketsService = new Intent(this, WebSocketService.class);
        startService(webSocketsService);
        bindService(webSocketsService, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void setUpBroadcastReceiver() {
        mBroadcastReceiver = new RoomBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.APP_PKG_NAME);

        registerReceiver(mBroadcastReceiver, filter);
    }

    private void handleIntentData(Intent data) {
        if (data != null) {
            if (data.hasExtra(KEY_ADDRESS)) {
                address = data.getStringExtra(KEY_ADDRESS);
            }

            if (data.hasExtra(KEY_AUTHTOKEN)) {
                token = data.getStringExtra(KEY_AUTHTOKEN);
            }
        }
    }

    private void setUpRecyclerView() {
        mRoomArrayList = mSqlHelper.getRooms();

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new RoomAdapter(mRoomArrayList, mOnRoomClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);
    }

    private void addRoom(int id, String name, long created) {
        mRoomArrayList.add(0, new Room(id, name, created));
        mAdapter.notifyDataSetChanged();
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
}
