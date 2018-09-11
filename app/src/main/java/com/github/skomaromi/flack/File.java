package com.github.skomaromi.flack;

class File {
    private int serverId;
    private String hash, name, sizeStr;

    public File(int serverId, String hash, String name, String sizeStr) {
        this.serverId = serverId;
        this.hash = hash;
        this.name = name;
        this.sizeStr = sizeStr;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSizeStr() {
        return sizeStr;
    }

    public void setSizeStr(String sizeStr) {
        this.sizeStr = sizeStr;
    }
}
