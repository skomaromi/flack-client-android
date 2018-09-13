package com.github.skomaromi.flack;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class RoomCreateAdapter extends RecyclerView.Adapter<RoomCreateAdapter.RoomCreateViewHolder> {
    private ArrayList<User> mUsers;
    private UserClickCallback mCallback;

    public RoomCreateAdapter(ArrayList<User> users, UserClickCallback callback) {
        mUsers = users;
        mCallback = callback;
    }

    @NonNull
    @Override
    public RoomCreateAdapter.RoomCreateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                                .from(parent.getContext())
                                .inflate(
                                        R.layout.activity_room_create_item,
                                        parent,
                                        false
                                );
        return new RoomCreateViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomCreateViewHolder holder, int position) {
        User user = mUsers.get(position);

        String name = user.getName();
        holder.userName.setText(name);
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class RoomCreateViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.roomcreate_tv_username) TextView userName;
        @BindView(R.id.roomcreate_cb_selected) CheckBox userCheckBox;

        private boolean mChecked;

        public RoomCreateViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            mChecked = false;
        }

        @OnClick
        public void onUserClick() {
            toggleItemState();
        }

        private void toggleItemState() {
            mChecked = !mChecked;
            mCallback.onClick(mUsers.get(getAdapterPosition()));
            updateItem();
        }

        private void updateItem() {
            userCheckBox.setChecked(mChecked);
            itemView.setActivated(mChecked);
        }
    }
}
