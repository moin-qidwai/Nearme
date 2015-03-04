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
public class UserRelationDbAdapter {

    public static final String KEY_USERNAME = "username";
    public static final String KEY_DISPLAYNAME = "display_name";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_TARGETID = "target_id";

    private static final String TAG = "UserRelationDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table user_relations (_id integer primary key autoincrement, "
                    + "target_id integer not null, username text not null, display_name text not null);";

    private static final String DATABASE_NAME = "relations";
    private static final String DATABASE_TABLE = "user_relations";
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
            db.execSQL("DROP TABLE IF EXISTS user_relations");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public UserRelationDbAdapter(Context ctx) {
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
    public UserRelationDbAdapter open() throws SQLException {
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
     * @param username the title of the note
     * @param targetId the body of the note
     * @return rowId or -1 if failed
     */
    public long createRelation(int targetId, String username, String displayName) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_USERNAME, username);
        initialValues.put(KEY_DISPLAYNAME, displayName);
        initialValues.put(KEY_TARGETID, targetId);

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
     * @param targetId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(int targetId) {

        return mDb.delete(DATABASE_TABLE, KEY_TARGETID + "=" + targetId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     *
     * @return Cursor over all notes
     */
    public Cursor fetchAllRelations() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TARGETID, KEY_USERNAME,
                KEY_DISPLAYNAME}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     *
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchRelation(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                                KEY_TARGETID, KEY_USERNAME, KEY_DISPLAYNAME}, KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Return a Cursor positioned at the note that matches the given userId
     *
     * @param targetId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchRelation(int targetId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TARGETID, KEY_USERNAME,
                                KEY_DISPLAYNAME}, KEY_TARGETID + "=" + targetId, null,
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
     * @param username value to set note title to
     * @param displayName value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateUser(long rowId, int targetId,  String username, String displayName) {
        ContentValues args = new ContentValues();
        args.put(KEY_TARGETID, targetId);
        args.put(KEY_USERNAME, username);
        args.put(KEY_DISPLAYNAME, displayName);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the userId, and it is altered to use the name and number
     * values passed in
     *
     * @param targetId id of note to update
     * @param username value to set note title to
     * @param displayName value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateUser(int targetId, String username, String displayName) {
        ContentValues args = new ContentValues();
        args.put(KEY_USERNAME, username);
        args.put(KEY_DISPLAYNAME, displayName);

        return mDb.update(DATABASE_TABLE, args, KEY_TARGETID + "=" + targetId, null) > 0;
    }
}
