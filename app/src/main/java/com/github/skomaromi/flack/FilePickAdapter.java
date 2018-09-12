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

class FilePickAdapter extends RecyclerView.Adapter<FilePickAdapter.FilePickViewHolder> {
    private ArrayList<File> mFiles;
    private FileClickCallback mCallback;

    public FilePickAdapter(ArrayList<File> files, FileClickCallback callback) {
        mFiles = files;
        mCallback = callback;
    }

    @NonNull
    @Override
    public FilePickAdapter.FilePickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                                .from(parent.getContext())
                                .inflate(
                                        R.layout.activity_file_pick_item,
                                        parent,
                                        false
                                );
        return new FilePickViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FilePickViewHolder holder, int position) {
        File file = mFiles.get(position);

        String name = file.getName();
        holder.fileName.setText(name);

        String sizeStr = file.getSizeStr();
        holder.fileSize.setText(sizeStr);
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class FilePickViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.filepick_tv_name) TextView fileName;
        @BindView(R.id.filepick_tv_sizevalue) TextView fileSize;

        public FilePickViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick
        public void onFileClick() {
            mCallback.onClick(mFiles.get(getAdapterPosition()));
        }
    }
}
