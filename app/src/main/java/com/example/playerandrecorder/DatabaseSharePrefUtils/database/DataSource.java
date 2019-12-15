/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.playerandrecorder.DatabaseSharePrefUtils.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import com.example.playerandrecorder.AppConstants;
import com.example.playerandrecorder.BuildConfig;

import java.util.ArrayList;
import java.util.List;


public abstract class DataSource<T> {


    protected SQLiteHelper dbHelper;


    protected SQLiteDatabase db;


    protected String tableName;

    private final String LOG_TAG = getClass().getSimpleName();


    public DataSource(Context context, String tableName) {
        dbHelper = new SQLiteHelper(context);
        this.tableName = tableName;
    }


    public void open() {
        db = dbHelper.getWritableDatabase();
    }


    public void close() {
        db.close();
        dbHelper.close();
    }

    public boolean isOpen() {
        return db != null && db.isOpen();
    }

    public T insertItem(T item) {
        ContentValues values = itemToContentValues(item);
        if (values != null) {
            int insertId = (int) db.insert(tableName, null, values);
            Log.d(LOG_TAG, "Insert into " + tableName + " id = " + insertId);
            return getItem(insertId);
        } else {
            Log.e(LOG_TAG, "Unable to write empty item!");
            return null;
        }
    }


    public abstract ContentValues itemToContentValues(T item);


    public void deleteItem(int id) {
        Log.d(LOG_TAG, tableName + " deleted ID = " + id);
        db.delete(tableName, SQLiteHelper.COLUMN_ID + " = " + id, null);
    }


    public int updateItem(T item) {
        ContentValues values = itemToContentValues(item);
        if (values != null && values.containsKey(SQLiteHelper.COLUMN_ID)) {
            String where = SQLiteHelper.COLUMN_ID + " = "
                    + values.get(SQLiteHelper.COLUMN_ID);
            int n = db.update(tableName, values, where, null);
            Log.d(LOG_TAG, "Updated records count = " + n);
            return n;
        } else {
            Log.e(LOG_TAG, "Unable to update empty item!");
            return 0;
        }
    }


    public ArrayList<T> getAll() {
        Cursor cursor = queryLocal("SELECT * FROM " + tableName + " ORDER BY " + SQLiteHelper.COLUMN_DATE_ADDED + " DESC");
        return convertCursor(cursor);
    }


    public ArrayList<T> getRecords(int page) {
        Cursor cursor = queryLocal("SELECT * FROM " + tableName
                + " ORDER BY " + SQLiteHelper.COLUMN_DATE_ADDED + " DESC"
                + " LIMIT " + AppConstants.DEFAULT_PER_PAGE
                + " OFFSET " + (page - 1) * AppConstants.DEFAULT_PER_PAGE);
        return convertCursor(cursor);
    }


    public ArrayList<T> getRecords(int page, String order) {
        Cursor cursor = queryLocal("SELECT * FROM " + tableName
                + " ORDER BY " + order
                + " LIMIT " + AppConstants.DEFAULT_PER_PAGE
                + " OFFSET " + (page - 1) * AppConstants.DEFAULT_PER_PAGE);
        return convertCursor(cursor);
    }


    public void deleteAll() throws SQLException {
        db.execSQL("DELETE FROM " + tableName);
    }


    public ArrayList<T> getItems(String where) {
        Cursor cursor = queryLocal("SELECT * FROM "
                + tableName + " WHERE " + where);
        return convertCursor(cursor);
    }


    public T getItem(int id) {
        Cursor cursor = queryLocal("SELECT * FROM " + tableName
                + " WHERE " + SQLiteHelper.COLUMN_ID + " = " + id);
        List<T> list = convertCursor(cursor);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }


    public ArrayList<T> convertCursor(Cursor cursor) {
        ArrayList<T> items = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast() && !cursor.isBeforeFirst()) {
            items.add(recordToItem(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        if (items.size() > 0) {
            return items;
        }
        return items;
    }


    public abstract T recordToItem(Cursor cursor);

    protected Cursor queryLocal(String query) {
        Log.d(LOG_TAG, "queryLocal: " + query);
        Cursor c = db.rawQuery(query, null);
        if (BuildConfig.DEBUG) {
            StringBuilder data = new StringBuilder("Cursor[");
            if (c.moveToFirst()) {
                do {
                    int columnCount = c.getColumnCount();
                    data.append("row[");
                    for (int i = 0; i < columnCount; ++i) {
                        data.append(c.getColumnName(i)).append(" = ");

                        switch (c.getType(i)) {
                            case Cursor.FIELD_TYPE_BLOB:
                                data.append("byte array");
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                data.append(c.getFloat(i));
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                data.append(c.getInt(i));
                                break;
                            case Cursor.FIELD_TYPE_NULL:
                                data.append("null");
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                data.append(c.getString(i));
                                break;
                        }
                        if (i != columnCount - 1) {
                            data.append(", ");
                        }
                    }
                    data.append("]\n");
                } while (c.moveToNext());
            }
            data.append("]");
            Log.d(LOG_TAG, data.toString());
        }
        return c;
    }

    //TODO: move this method
    public List<Long> getRecordsDurations() {
        Cursor cursor = queryLocal("SELECT " + SQLiteHelper.COLUMN_DURATION + " FROM " + tableName);
        ArrayList<Long> items = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            items.add(cursor.getLong(cursor.getColumnIndex(SQLiteHelper.COLUMN_DURATION)));
            cursor.moveToNext();
        }
        cursor.close();
        if (items.size() > 0) {
            return items;
        }
        return items;
    }
}
