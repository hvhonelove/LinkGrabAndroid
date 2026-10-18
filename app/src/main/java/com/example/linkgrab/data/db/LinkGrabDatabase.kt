package com.example.linkgrab.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = true)
@TypeConverters(EnumConverters::class)
abstract class LinkGrabDatabase : RoomDatabase() { abstract fun downloads(): DownloadDao }
