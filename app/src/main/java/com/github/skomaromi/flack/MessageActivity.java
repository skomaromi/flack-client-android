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
import android.util.Log;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MessageActivity extends AppCompatActivity {
    @BindView(R.id.message_rv) RecyclerView recyclerView;

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
        }
    };

    private MessageClickCallback mOnLocationClickCallback = new MessageClickCallback() {
        @Override
        public void onClick(Message message) {
            // TODO: do something here, like opening Maps.
        }
    };

    private class MessageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int objectType = intent.getIntExtra(WebSocketService.KEY_OBJECTTYPE, -1);

            if (objectType == WebSocketService.TYPE_MESSAGE) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        ButterKnife.bind(this);

        Intent data = getIntent();
        handleIntentData(data);

        mSqlHelper = new SqlHelper(this);
        setUpRecyclerView();
        setUpBroadcastReceiver();
        startWebSocketsService();

        Log.d(Constants.APP_NAME, String.format("MessageActivity started with roomId '%d'!", roomId));
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
        mService.setCurrentRoom(WebSocketService.NOT_ON_MESSAGE_ACTIVITY);
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
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

    private void setUpRecyclerView() {
        mMessageArrayList = mSqlHelper.getMessages(roomId);

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new MessageAdapter(mMessageArrayList, mOnFileClickCallback, mOnLocationClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);
    }

    private void addMessage(String sender, String content, long timeCreated, Location location, MessageFile file) {
        mMessageArrayList.add(new Message(sender, content, timeCreated, location, file));
        mAdapter.notifyDataSetChanged();
    }

    private void startWebSocketsService() {
        Intent webSocketsService = new Intent(this, WebSocketService.class);
        startService(webSocketsService);
    }
}
