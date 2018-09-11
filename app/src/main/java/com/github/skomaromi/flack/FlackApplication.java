package com.github.skomaromi.flack;

import android.app.Application;

public class FlackApplication extends Application {
    public static final int NO_ACTIVITY_VISIBLE = -1;
    public static final int ON_ROOM_LIST = -2;

    private static boolean isAnyActivityVisible = false;
    private static int currentRoom;

    public static boolean isAnyActivityVisible() {
        return isAnyActivityVisible;
    }

    public static int getCurrentRoom() {
        return currentRoom;
    }

    public static void setCurrentRoom(int currentRoom) {
        FlackApplication.currentRoom = currentRoom;

        if (currentRoom == NO_ACTIVITY_VISIBLE) {
            isAnyActivityVisible = false;
        }
    }

    public static boolean checkIsOnRoomList() {
        return currentRoom == ON_ROOM_LIST;
    }
}
