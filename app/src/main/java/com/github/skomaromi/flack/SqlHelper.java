package com.github.skomaromi.flack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Locale;

class SqlHelper extends SQLiteOpenHelper {

    private class RoomEntry implements BaseColumns {
        public static final String TABLE = "rooms";
        public static final String COL_SERVERID = "server_id";
        public static final String COL_NAME = "name";
        public static final String COL_TIMECREATED = "time_created";
        public static final String COL_LASTMESSAGETEXT = "last_message_text";
        public static final String COL_TIMELASTMESSAGE = "time_last_message";
        public static final String COL_TIMEMODIFIED = "time_modified";
    }

    private class MessageEntry implements BaseColumns {
        public static final String TABLE = "messages";
        public static final String COL_ROOM_SERVERID = "room_server_id";
        public static final String COL_SENDERNAME = "sender_name";
        public static final String COL_CONTENT = "content";
        public static final String COL_TIMECREATED = "time_created";

        // location
        public static final String COL_LOCATION_LATITUDE = "loc_latitude";
        public static final String COL_LOCATION_LONGITUDE = "loc_longitude";

        // file
        public static final String COL_FILE_HASH = "file_hash";
        public static final String COL_FILE_NAME = "file_name";
    }

    public SqlHelper(Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createRoomTableQuery = "CREATE TABLE " + RoomEntry.TABLE + " (" +
                RoomEntry._ID +                 " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RoomEntry.COL_SERVERID +        " INTEGER NOT NULL, " +
                RoomEntry.COL_NAME +            " TEXT NOT NULL, " +
                RoomEntry.COL_TIMECREATED +     " BIGINT NOT NULL, " +
                RoomEntry.COL_LASTMESSAGETEXT + " TEXT, " +
                RoomEntry.COL_TIMELASTMESSAGE + " BIGINT, " +
                RoomEntry.COL_TIMEMODIFIED +    " BIGINT NOT NULL" +
        ");";
        String createMessageTableQuery = "CREATE TABLE " + MessageEntry.TABLE + " (" +
                MessageEntry._ID +                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MessageEntry.COL_ROOM_SERVERID +      " INTEGER NOT NULL, " +
                MessageEntry.COL_SENDERNAME +         " TEXT NOT NULL, " +
                MessageEntry.COL_CONTENT +            " TEXT, " +
                MessageEntry.COL_TIMECREATED +        " BIGINT NOT NULL, " +
                MessageEntry.COL_LOCATION_LATITUDE +  " REAL, " +
                MessageEntry.COL_LOCATION_LONGITUDE + " REAL, " +
                MessageEntry.COL_FILE_HASH +          " VARCHAR(128), " +
                MessageEntry.COL_FILE_NAME +          " VARCHAR(255)" +
        ");";

        db.execSQL(createRoomTableQuery);
        db.execSQL(createMessageTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RoomEntry.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + MessageEntry.TABLE);
        onCreate(db);
    }

    private Room makeRoom(Cursor cursor) {
        return new Room(
                cursor.getInt(cursor.getColumnIndex(RoomEntry.COL_SERVERID)),
                cursor.getString(cursor.getColumnIndex(RoomEntry.COL_NAME)),
                cursor.getLong(cursor.getColumnIndex(RoomEntry.COL_TIMECREATED)),
                cursor.getString(cursor.getColumnIndex(RoomEntry.COL_LASTMESSAGETEXT)),
                cursor.getLong(cursor.getColumnIndex(RoomEntry.COL_TIMELASTMESSAGE))
        );
    }

    private Message makeMessage(Cursor cursor) {
        String sender = cursor.getString(cursor.getColumnIndex(MessageEntry.COL_SENDERNAME));
        String content = cursor.getString(cursor.getColumnIndex(MessageEntry.COL_CONTENT));
        long timeCreated = cursor.getLong(cursor.getColumnIndex(MessageEntry.COL_TIMECREATED));

        Location location = null;
        if (!cursor.isNull(cursor.getColumnIndex(MessageEntry.COL_LOCATION_LATITUDE))) {
            float latitude = cursor.getFloat(cursor.getColumnIndex(MessageEntry.COL_LOCATION_LATITUDE));
            float longitude = cursor.getFloat(cursor.getColumnIndex(MessageEntry.COL_LOCATION_LONGITUDE));

            location = new Location(latitude, longitude);
        }

        MessageFile file = null;
        if (!cursor.isNull(cursor.getColumnIndex(MessageEntry.COL_FILE_HASH))) {
            // TODO: see if isNull is enough.
            String hash = cursor.getString(cursor.getColumnIndex(MessageEntry.COL_FILE_HASH));
            String name = cursor.getString(cursor.getColumnIndex(MessageEntry.COL_FILE_NAME));

            file = new MessageFile(hash, name);
        }

        return new Message(
                sender,
                content,
                timeCreated,
                location,
                file
        );
    }

    public ArrayList<Room> getRooms() {
        ArrayList<Room> rooms = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();

        if (database != null && database.isOpen()) {
            String query = "SELECT * FROM " + RoomEntry.TABLE + " ORDER BY " + RoomEntry.COL_TIMEMODIFIED + " DESC";
            Cursor cursor = database.rawQuery(query, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    rooms.add(makeRoom(cursor));
                    cursor.moveToNext();
                }
            }

            database.close();
        }

        return rooms;
    }

    public Room getRoom(int roomId) {
        SQLiteDatabase database = getReadableDatabase();

        if (database != null && database.isOpen()) {
            String query = "SELECT * FROM " + RoomEntry.TABLE + " WHERE " + RoomEntry.COL_SERVERID + " = " + roomId;
            Cursor cursor = database.rawQuery(query, null);
            if (cursor != null) {
                cursor.moveToFirst();
                return makeRoom(cursor);
            }

            database.close();
        }

        return null;
    }

    public ArrayList<Message> getMessages(int roomId) {
        ArrayList<Message> messages = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();

        if (database != null && database.isOpen()) {
            String query = String.format(
                    Locale.ENGLISH,
                    "SELECT * FROM %s WHERE %s = %d ORDER BY %s ASC",
                    MessageEntry.TABLE,
                    MessageEntry.COL_ROOM_SERVERID,
                    roomId,
                    MessageEntry.COL_TIMECREATED
            );
            Cursor cursor = database.rawQuery(query, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    messages.add(makeMessage(cursor));
                    cursor.moveToNext();
                }
            }

            database.close();
        }

        return messages;
    }

    public boolean addRoom(int serverId, String name, long timeCreated) {
        long id;
        SQLiteDatabase database = getWritableDatabase();

        if (database != null && database.isOpen()) {
            ContentValues values = new ContentValues();
            values.put(RoomEntry.COL_SERVERID, serverId);
            values.put(RoomEntry.COL_NAME, name);
            values.put(RoomEntry.COL_TIMECREATED, timeCreated);
            values.put(RoomEntry.COL_TIMEMODIFIED, timeCreated);

            id = database.insert(RoomEntry.TABLE, null, values);

            database.close();
        }
        else return false;

        return id != -1;
    }

    public boolean addMessage(int roomId, Message message) {
        long id;
        SQLiteDatabase database = getWritableDatabase();

        if (database != null && database.isOpen()) {
            ContentValues values = new ContentValues();

            values.put(MessageEntry.COL_ROOM_SERVERID, roomId);

            String sender = message.getSender();
            values.put(MessageEntry.COL_SENDERNAME, sender);

            String content = message.getContent();
            values.put(MessageEntry.COL_CONTENT, content);

            long timeCreated = message.getTimeCreated();
            values.put(MessageEntry.COL_TIMECREATED, timeCreated);

            Location location = message.getLocation();
            if (location != null) {
                values.put(MessageEntry.COL_LOCATION_LATITUDE, location.getLatitude());
                values.put(MessageEntry.COL_LOCATION_LONGITUDE, location.getLongitude());
            }

            MessageFile file = message.getFile();
            if (file != null) {
                values.put(MessageEntry.COL_FILE_HASH, file.getHash());
                values.put(MessageEntry.COL_FILE_NAME, file.getName());
            }

            id = database.insert(MessageEntry.TABLE, null, values);

            // update Room as well
            String roomLastMessageContent = message.toString();
            roomLastMessageContent = DatabaseUtils.sqlEscapeString(roomLastMessageContent);
            String roomUpdateQuery =
                    "UPDATE " + RoomEntry.TABLE + " " +
                    "SET " + RoomEntry.COL_LASTMESSAGETEXT + " = CASE WHEN " + RoomEntry.COL_TIMEMODIFIED + " < " + timeCreated + " THEN " + roomLastMessageContent + " ELSE " + RoomEntry.COL_LASTMESSAGETEXT + " END, " +
                             RoomEntry.COL_TIMELASTMESSAGE + " = CASE WHEN " + RoomEntry.COL_TIMEMODIFIED + " < " + timeCreated + " THEN " + timeCreated +            " ELSE " + RoomEntry.COL_TIMELASTMESSAGE + " END, " +
                             RoomEntry.COL_TIMEMODIFIED +    " = CASE WHEN " + RoomEntry.COL_TIMEMODIFIED + " < " + timeCreated + " THEN " + timeCreated +            " ELSE " + RoomEntry.COL_TIMEMODIFIED +    " END " +
                    "WHERE " + RoomEntry.COL_SERVERID + " = " + roomId;
            database.execSQL(roomUpdateQuery);

            database.close();
        }
        else return false;

        return id != -1;
    }
}
