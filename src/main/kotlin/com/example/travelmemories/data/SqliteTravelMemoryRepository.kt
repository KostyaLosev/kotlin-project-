package com.example.travelmemories.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_COUNTRY
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_ID
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_NOTE
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_PHOTO_URI
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_RATING
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_TITLE
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_TYPE
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.COLUMN_VISIT_DATE
import com.example.travelmemories.data.TravelMemoryDatabaseHelper.Companion.TABLE_MEMORIES

class SqliteTravelMemoryRepository(context: Context) : TravelMemoryRepository {
    private val helper = TravelMemoryDatabaseHelper(context)

    override fun getAll(): List<TravelMemory> {
        val memories = mutableListOf<TravelMemory>()
        helper.readableDatabase.query(
            TABLE_MEMORIES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_VISIT_DATE DESC, $COLUMN_TITLE COLLATE NOCASE ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                memories += cursor.toMemory()
            }
        }
        return memories
    }

    override fun getById(id: Long): TravelMemory? {
        helper.readableDatabase.query(
            TABLE_MEMORIES,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toMemory() else null
        }
    }

    override fun save(memory: TravelMemory): Long {
        val values = ContentValues().apply {
            put(COLUMN_TYPE, memory.type.name)
            put(COLUMN_TITLE, memory.title)
            put(COLUMN_COUNTRY, memory.country)
            put(COLUMN_VISIT_DATE, memory.visitDate)
            put(COLUMN_RATING, memory.rating)
            put(COLUMN_NOTE, memory.note)
            put(COLUMN_PHOTO_URI, memory.photoUri)
        }

        return if (memory.id == 0L) {
            helper.writableDatabase.insert(TABLE_MEMORIES, null, values)
        } else {
            helper.writableDatabase.update(
                TABLE_MEMORIES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(memory.id.toString()),
            )
            memory.id
        }
    }

    override fun delete(id: Long) {
        helper.writableDatabase.delete(
            TABLE_MEMORIES,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
        )
    }

    private fun Cursor.toMemory(): TravelMemory {
        return TravelMemory(
            id = getLong(getColumnIndexOrThrow(COLUMN_ID)),
            type = MemoryType.valueOf(getString(getColumnIndexOrThrow(COLUMN_TYPE))),
            title = getString(getColumnIndexOrThrow(COLUMN_TITLE)),
            country = getString(getColumnIndexOrThrow(COLUMN_COUNTRY)),
            visitDate = getString(getColumnIndexOrThrow(COLUMN_VISIT_DATE)),
            rating = getInt(getColumnIndexOrThrow(COLUMN_RATING)),
            note = getString(getColumnIndexOrThrow(COLUMN_NOTE)),
            photoUri = getString(getColumnIndexOrThrow(COLUMN_PHOTO_URI)),
        )
    }
}
