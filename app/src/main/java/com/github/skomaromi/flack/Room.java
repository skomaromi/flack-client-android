package com.github.skomaromi.flack;

import android.support.annotation.NonNull;

class Room implements Comparable<Room> {
    private int serverId;
    private String name;
    private long timeCreated;

    // Message-related data
    private String lastMessageText;
    private long timeLastMessage;

    private long timeModified;

    public Room(int serverId, String name, long timeCreated) {
        this.serverId = serverId;
        this.name = name;
        this.timeCreated = timeCreated;
        this.lastMessageText = null;
        this.timeLastMessage = -1;
        this.timeModified = timeCreated;
    }

    public Room(int serverId, String name, long timeCreated, String lastMessageText, long timeLastMessage) {
        this.serverId = serverId;
        this.name = name;
        this.timeCreated = timeCreated;
        this.lastMessageText = lastMessageText;
        this.timeLastMessage = timeLastMessage;
        this.timeModified = timeLastMessage > timeCreated? timeLastMessage : timeCreated;
    }

    @Override
    public int compareTo(@NonNull Room o) {
        return Long.compare(this.getTimeModified(), o.getTimeModified());
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public long getTimeLastMessage() {
        return timeLastMessage;
    }

    public void setTimeLastMessage(long timeLastMessage) {
        this.timeLastMessage = timeLastMessage;
    }

    public long getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }
}
