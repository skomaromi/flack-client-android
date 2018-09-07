package com.github.skomaromi.flack;

import java.util.ArrayList;

class Room {
    private int id;
    private String name;
    private String timeCreated;
    private String timeLastMessage;
    private ArrayList<Integer> participants;

    public Room() {}

    public Room(int id, String name, String timeCreated,
                String timeLastMessage, ArrayList<Integer> participants) {
        this.id = id;
        this.name = name;
        this.timeCreated = timeCreated;
        this.timeLastMessage = timeLastMessage;
        this.participants = participants;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getTimeLastMessage() {
        return timeLastMessage;
    }

    public void setTimeLastMessage(String timeLastMessage) {
        this.timeLastMessage = timeLastMessage;
    }

    public ArrayList<Integer> getParticipants() {
        return participants;
    }

    public void setParticipants(ArrayList<Integer> participants) {
        this.participants = participants;
    }
}
