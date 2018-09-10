package com.github.skomaromi.flack;

class Message {
    private String sender;
    private String content;
    private long timeCreated;
    private Location location;
    private MessageFile file;

    public Message(String sender, String content, long timeCreated, Location location, MessageFile file) {
        this.sender = sender;
        this.content = content;
        this.timeCreated = timeCreated;
        this.location = location;
        this.file = file;
    }

    @Override
    public String toString() {
        if (content != null && !content.isEmpty()) {
            return content;
        }
        else {
            return file.getName();
        }
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public MessageFile getFile() {
        return file;
    }

    public void setFile(MessageFile file) {
        this.file = file;
    }
}
