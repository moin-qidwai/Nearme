/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nearme.moin.nearme;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class UserDbAdapter {

    public static final String KEY_NAME = "name";
    public static final String KEY_DISPLAYNAME = "display_name";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_USERID = "userid";
    public static final String KEY_COUNTRY = "country";

    private static final String TAG = "UserDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table info (_id integer primary key autoincrement, "
        + "userid integer not null, name text, number text not null, country text not null, display_name text);";

    private static final String DATABASE_NAME = "users";
    private static final String DATABASE_TABLE = "info";
    private static final int DATABASE_VERSION = 3;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS info");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public UserDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public UserDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param name the title of the note
     * @param number the body of the note
     * @return rowId or -1 if failed
     */
    public long createUser(int userId, String name, String number, String country, String displayName) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_NUMBER, number);
        initialValues.put(KEY_USERID, userId);
        initialValues.put(KEY_COUNTRY, country);
        initialValues.put(KEY_DISPLAYNAME, displayName);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Delete the note with the given userId
     *
     * @param userId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(int userId) {

        return mDb.delete(DATABASE_TABLE, KEY_USERID + "=" + userId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllUsers() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_USERID, KEY_NAME,
                KEY_NUMBER, KEY_COUNTRY, KEY_DISPLAYNAME}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchUser(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                            KEY_NAME, KEY_NUMBER, KEY_COUNTRY, KEY_DISPLAYNAME}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Return a Cursor positioned at the note that matches the given userId
     *
     * @param userId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchUser(int userId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_USERID,
                                KEY_NAME, KEY_NUMBER, KEY_COUNTRY, KEY_DISPLAYNAME}, KEY_USERID + "=" + userId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param name value to set note title to
     * @param number value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateUser(long rowId, String name, String number, String country, String displayName) {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        args.put(KEY_NUMBER, number);
        args.put(KEY_COUNTRY, country);
        args.put(KEY_DISPLAYNAME, displayName);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the userId, and it is altered to use the name and number
     * values passed in
     *
     * @param userId id of note to update
     * @param name value to set note title to
     * @param number value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateUser(int userId, String name, String number, String country, String displayName) {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        args.put(KEY_NUMBER, number);
        args.put(KEY_COUNTRY, country);
        args.put(KEY_DISPLAYNAME, displayName);

        return mDb.update(DATABASE_TABLE, args, KEY_USERID + "=" + userId, null) > 0;
    }
}
