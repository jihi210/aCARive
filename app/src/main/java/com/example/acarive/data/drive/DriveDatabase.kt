package com.example.acarive.data.drive

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase

@Database(entities = [DriveSession::class, RoutePoint::class], version = 1)
abstract class DriveDatabase: RoomDatabase() {
    abstract fun driveDao(): DriveDao

    companion object{
        @Volatile private var INSTANCE: DriveDatabase?=null

        fun get(context: Context): DriveDatabase=
            INSTANCE ?:synchronized(this){
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,//앱 신분증
                    DriveDatabase::class.java,
                    "drive.db"
                ).build().also { INSTANCE = it }
            }
    }
}