package com.github.skomaromi.flack;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private ArrayList<Message> mMessages;
    private MessageClickCallback mFileCallback, mLocationCallback;

    public MessageAdapter(ArrayList<Message> messages, MessageClickCallback fileCallback, MessageClickCallback locationCallback) {
        mMessages = messages;
        mFileCallback = fileCallback;
        mLocationCallback = locationCallback;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                                .from(parent.getContext())
                                .inflate(
                                        R.layout.activity_message_item,
                                        parent,
                                        false
                                );

        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = mMessages.get(position);

        String sender = message.getSender();
        holder.sender.setText(sender);

        long time = message.getTimeCreated();
        DateFormat format = new SimpleDateFormat("d MMM 'at' HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String timeStr = format.format(calendar.getTime());
        holder.time.setText(timeStr);

        Location location = message.getLocation();
        if (location != null) {
            holder.location.setText(location.toString());
        }
        else {
            holder.locationContainer.setVisibility(View.GONE);
        }

        String content = message.getContent();
        if (content != null && !content.isEmpty()) {
            holder.content.setText(content);
        }
        else {
            holder.content.setVisibility(View.GONE);
        }

        MessageFile file = message.getFile();
        if (file != null) {
            holder.file.setText(file.getName());
        }
        else {
            holder.fileContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_tv_sender) TextView sender;
        @BindView(R.id.message_tv_time) TextView time;

        @BindView(R.id.message_rl_location) RelativeLayout locationContainer;
        @BindView(R.id.message_tv_location) TextView location;

        @BindView(R.id.message_tv_messagecontent) TextView content;

        @BindView(R.id.message_rl_file) RelativeLayout fileContainer;
        @BindView(R.id.message_tv_filename) TextView file;

        public MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.message_rl_file)
        public void onFileClick() {
            mFileCallback.onClick(mMessages.get(getAdapterPosition()));
        }

        @OnClick(R.id.message_rl_location)
        public void onLocationClick() {
            mLocationCallback.onClick(mMessages.get(getAdapterPosition()));
        }
    }
}
