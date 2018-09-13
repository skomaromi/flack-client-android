package com.github.skomaromi.flack;

class User {
    private int serverId;
    private String name;

    public User(int serverId, String name) {
        this.serverId = serverId;
        this.name = name;
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
}
