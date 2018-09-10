package com.github.skomaromi.flack;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
    private ArrayList<Room> mRooms;
    private RoomClickCallback mCallback;

    public RoomAdapter(ArrayList<Room> rooms, RoomClickCallback callback) {
        mRooms = rooms;
        mCallback = callback;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {
        View itemView = LayoutInflater
                                .from(parent.getContext())
                                .inflate(
                                        R.layout.activity_room_item,
                                        parent,
                                        false
                                );

        return new RoomViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = mRooms.get(position);
        String name = room.getName();

        long time = room.getTimeModified();

        DateFormat format = new SimpleDateFormat("d MMM");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String timeStr = format.format(calendar.getTime());

        String message = room.getLastMessageText();
        message = message == null? "(no messages yet)" : message;

        holder.name.setText(name);
        holder.time.setText(timeStr);
        holder.message.setText(message);
    }

    @Override
    public int getItemCount() {
        return mRooms.size();
    }

    public class RoomViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.rooms_tv_roomname) TextView name;
        @BindView(R.id.rooms_tv_lastmodified) TextView time;
        @BindView(R.id.rooms_tv_lastmessage) TextView message;

        public RoomViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick
        public void onRoomClick() {
            mCallback.onClick(mRooms.get(getAdapterPosition()));
        }
    }
}
