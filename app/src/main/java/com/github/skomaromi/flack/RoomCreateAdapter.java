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
    private boolean[] mItemStates;
    private UserClickCallback mCallback;

    public RoomCreateAdapter(ArrayList<User> users, UserClickCallback callback) {
        mUsers = users;
        mCallback = callback;

        mItemStates = new boolean[mUsers.size()];
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

        if (position < mItemStates.length) {
            holder.userCheckBox.setChecked(mItemStates[position]);
        }
        else if (mItemStates.length != mUsers.size()) {
            // mUsers.size() changed, and we weren't notified about it
            mItemStates = new boolean[mUsers.size()];
        }

    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class RoomCreateViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.roomcreate_tv_username) TextView userName;
        @BindView(R.id.roomcreate_cb_selected) CheckBox userCheckBox;

        public RoomCreateViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick
        public void onUserClick() {
            toggleItemState();
        }

        private void toggleItemState() {
            int pos = getAdapterPosition();
            mItemStates[pos] = !mItemStates[pos];
            updateItem(mItemStates[pos]);
            mCallback.onClick(mUsers.get(pos));
        }

        private void updateItem(boolean state) {
            userCheckBox.setChecked(state);
            itemView.setActivated(state);
        }
    }
}
