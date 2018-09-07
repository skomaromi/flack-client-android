package com.github.skomaromi.flack;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RoomsActivity extends AppCompatActivity {
    @BindView(R.id.rooms_rv) RecyclerView recyclerView;

    public static final String KEY_ADDRESS = "address";
    public static final String KEY_AUTHTOKEN = "authtoken";

    private String address, token;

    private ArrayList<Room> mRoomArrayList;
    private RoomsAdapter mAdapter;

    private RoomClickCallback mOnRoomClickCallback = new RoomClickCallback() {
        @Override
        public void onClick(Room room) {
            Intent messagesActivity = new Intent(
                    RoomsActivity.this, MessagesActivity.class
            );

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        ButterKnife.bind(this);

        Intent data = getIntent();
        handleIntentData(data);

        setUpRecyclerView();
        fetchRooms();
        startWebSocketsService();
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
        mRoomArrayList = new ArrayList<>();

        LinearLayoutManager linearLayout = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        mAdapter = new RoomsAdapter(mRoomArrayList, mOnRoomClickCallback);

        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);
    }

    private void fetchRooms() {
        // TODO: should fetch rooms AND messages, store them in DB and then store last message|room in sharedprefs
        BackgroundRoomFetchTask t = new BackgroundRoomFetchTask();
        t.execute();
    }

    private class BackgroundRoomFetchTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            FlackApi api = new FlackApi(address, token);
            // TODO: replace this with calls to DB.
            // api.sync()
            // DbProvider.getRooms()
            JSONArray roomsJson = api.getRoomsJson();

            try {
                for (int i = 0; i < roomsJson.length(); i++) {
                    JSONObject roomJson = (JSONObject)roomsJson.get(i);
                    Room room = new Room();

                    room.setId(roomJson.getInt("id"));
                    room.setName(roomJson.getString("name"));
                    room.setTimeCreated(roomJson.getString("time_created"));
                    room.setTimeLastMessage("");

                    ArrayList<Integer> participants = new ArrayList<>();
                    JSONArray participantsJson = roomJson.getJSONArray(
                            "participants"
                    );

                    for (int j = 0; j < participantsJson.length(); j++) {
                        int participantId = (int)participantsJson.get(j);

                        participants.add(participantId);
                    }

                    room.setParticipants(participants);

                    mRoomArrayList.add(room);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void startWebSocketsService() {
        Intent webSocketsService = new Intent(this, WebSocketsService.class);
        webSocketsService.putExtra(WebSocketsService.KEY_ADDRESS, address);
        webSocketsService.putExtra(WebSocketsService.KEY_AUTHTOKEN, token);
        startService(webSocketsService);
    }
}
