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

class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private ArrayList<File> mFiles;
    private FileClickCallback mDownloadCallback, mHashCallback, mUrlCallback;

    public FileAdapter(ArrayList<File> files, FileClickCallback downloadCallback, FileClickCallback hashCallback, FileClickCallback urlCallback) {
        mFiles = files;
        mDownloadCallback = downloadCallback;
        mHashCallback = hashCallback;
        mUrlCallback = urlCallback;
    }

    @NonNull
    @Override
    public FileAdapter.FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                                .from(parent.getContext())
                                .inflate(
                                        R.layout.activity_file_item,
                                        parent,
                                        false
                                );
        return new FileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FileAdapter.FileViewHolder holder, int position) {
        File file = mFiles.get(position);

        String name = file.getName();
        holder.fileName.setText(name);

        String sizeStr = file.getSizeStr();
        holder.fileSize.setText(sizeStr);

        String hash = file.getHash();
        holder.fileHash.setText(hash);
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class FileViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.file_tv_name) TextView fileName;
        @BindView(R.id.file_tv_sizevalue) TextView fileSize;
        @BindView(R.id.file_tv_hash) TextView fileHash;

        public FileViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.file_btn_download)
        public void onDownloadClick() {
            mDownloadCallback.onClick(mFiles.get(getAdapterPosition()));
        }

        @OnClick(R.id.file_btn_hash)
        public void onHashClick() {
            mHashCallback.onClick(mFiles.get(getAdapterPosition()));
        }

        @OnClick(R.id.file_btn_url)
        public void onUrlClick() {
            mUrlCallback.onClick(mFiles.get(getAdapterPosition()));
        }
    }
}
