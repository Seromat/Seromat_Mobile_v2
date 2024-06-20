package com.example.seromatupgrade.DatabaseManager

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper



data class Measurement(
    val id: Long,
    val date: String,
    val temperature: Double,
    val humidity: Double)

class DatabaseManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "weather_data.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "measurements"
        const val COLUMN_ID = "id"
        const val COLUMN_DATE = "date"
        const val COLUMN_TEMPERATURE = "temperature"
        const val COLUMN_HUMIDITY = "humidity"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_DATE TEXT," +
                "$COLUMN_TEMPERATURE REAL," +
                "$COLUMN_HUMIDITY REAL)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getLatestMeasurements(): List<Measurement> {
        val limit  = 10 //chcemy 10 pomiarów
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DATE DESC", // desc = descending order
            limit.toString()
        )

        val measurements = mutableListOf<Measurement>()
        //Używamy cursora zamiast query
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val temperature = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TEMPERATURE))
            val humidity = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HUMIDITY))
            measurements.add(Measurement(id, date, temperature, humidity))
        }
        cursor.close()
        return measurements
    }

    fun insertMeasurement(date: String, temperature: Double, humidity: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_TEMPERATURE, temperature)
            put(COLUMN_HUMIDITY, humidity)
        }
        db.insert(TABLE_NAME, null, values)
    }
}
