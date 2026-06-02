package com.uglydeveloper.ulinhub

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Моделька для нашей ссылки
data class LinkModel(val id: Int, val url: String, val title: String)

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "LinkStash.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Создаем простую таблицу: id, сама ссылка (url) и красивое название (title)
        db.execSQL("CREATE TABLE links (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, title TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS links")
        onCreate(db)
    }

    // Метод для добавления новой ссылки
    fun addLink(url: String, title: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("url", url)
            put("title", title)
        }
        val result = db.insert("links", null, contentValues)
        return result != -1L
    }

    // Метод для вытаскивания всех ссылок из памяти телефона
    fun getAllLinks(): List<LinkModel> {
        val list = mutableListOf<LinkModel>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM links ORDER BY id DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val url = cursor.getString(1)
                val title = cursor.getString(2)
                list.add(LinkModel(id, url, title))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // Метод для удаления ссылки (пригодится для свайпов)
    fun deleteLink(id: Int) {
        val db = this.writableDatabase
        db.delete("links", "id = ?", arrayOf(id.toString()))
    }

    fun updateLink(id: Int, newUrl: String, newTitle: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("url", newUrl)
            put("title", newTitle)
        }
        // Обновляем запись, где id равен переданному
        val result = db.update("links", contentValues, "id = ?", arrayOf(id.toString()))
        return result > 0
    }
}
