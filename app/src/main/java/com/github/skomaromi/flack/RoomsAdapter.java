package com.github.skomaromi.flack;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.RoomViewHolder> {
    private ArrayList<Room> mRooms;
    private RoomClickCallback mCallback;

    public RoomsAdapter(ArrayList<Room> rooms, RoomClickCallback callback) {
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
                                        R.layout.activity_rooms_item,
                                        parent,
                                        false
                                );

        return new RoomViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = mRooms.get(position);
        holder.name.setText(room.getName());
    }


    @Override
    public int getItemCount() {
        return mRooms.size();
    }

    public class RoomViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.rooms_tv_roomname) TextView name;

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
