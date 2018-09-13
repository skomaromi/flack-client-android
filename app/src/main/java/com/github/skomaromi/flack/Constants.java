package com.github.skomaromi.flack;

public class Constants {
    public static final String APP_NAME = "Flack";
    public static final String APP_PKG_NAME = "com.github.skomaromi.flack";

    public static final int REQCODE_ACTIVITY_AUTH = 1;
    public static final int REQCODE_ACTIVITY_SERVERINPUT = 2;
    public static final int REQCODE_ACTIVITY_FILEPICKER = 3;
    public static final int REQCODE_ACTIVITY_ROOMCREATE = 4;

    /*
     * special request codes. kept here to prevent accidentally having the same request code for
     * different purposes
     */
    public static final int REQCODE_ACTIVITY_ANDROID_FILEPICKER = 5;
    public static final int REQCODE_PERMISSION_LOCATION = 6;
    public static final int REQCODE_PERMISSION_READ = 7;

    public static final String SERVER_PROTO = "http";
    public static final String SERVER_DEFAULT_ADDR = "flackserver";
    public static final String SERVER_DEFAULT_ADDR_ALT = "flackserver.local";
    public static final int SERVER_PORT = 8000;
    public static final int SERVER_IPFS_PORT = 8080;

    public static final String DB_NAME = "com.github.skomaromi.flack.db";
    public static final int DB_VERSION = 1;
}
