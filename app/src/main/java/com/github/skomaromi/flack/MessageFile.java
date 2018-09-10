package com.github.skomaromi.flack;

class MessageFile {
    private String hash, name;

    public MessageFile(String hash, String name) {
        this.hash = hash;
        this.name = name;
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
}