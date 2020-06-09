package com.example.spaigh.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
/* This class creates the database object and implements a singleton pattern for handles to
    the database
 */
@Database(entities = [Data::class],version = 1,exportSchema = false )
abstract class AppDatabase: RoomDatabase() {
    abstract fun dataDao(): DataDao

    private class RoomCallBack(
        private val scope: CoroutineScope
    ): RoomDatabase.Callback(){
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let{database ->
                scope.launch {
                    var dataDao = database.dataDao()
                    dataDao.deleteAllData() //delete all data in SQLite database on startup
                }
            }
        }
    }

    companion object {
        /* Singleton prevents multiple instances of database opening at the
         same time.*/
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "data_database"
                ).addCallback(RoomCallBack(scope))
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}