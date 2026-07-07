package com.example.travelmemories.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TravelMemoryDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MEMORIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_COUNTRY TEXT NOT NULL,
                $COLUMN_VISIT_DATE TEXT NOT NULL,
                $COLUMN_RATING INTEGER NOT NULL,
                $COLUMN_NOTE TEXT NOT NULL,
                $COLUMN_PHOTO_URI TEXT
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_MEMORIES ADD COLUMN $COLUMN_PHOTO_URI TEXT")
        }
    }

    companion object {
        const val DATABASE_NAME = "travel_memories.db"
        const val DATABASE_VERSION = 2

        const val TABLE_MEMORIES = "travel_memories"
        const val COLUMN_ID = "id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_TITLE = "title"
        const val COLUMN_COUNTRY = "country"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_RATING = "rating"
        const val COLUMN_NOTE = "note"
        const val COLUMN_PHOTO_URI = "photo_uri"
    }
}
