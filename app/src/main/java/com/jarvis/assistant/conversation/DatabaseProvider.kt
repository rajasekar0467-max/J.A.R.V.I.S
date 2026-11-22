package com.jarvis.assistant.conversation

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var db: JarvisDatabase? = null

    fun get(context: Context): JarvisDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                JarvisDatabase::class.java,
                "jarvis.db"
            ).build().also { db = it }
        }
    }
}
