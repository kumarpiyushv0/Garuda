package com.example.garuda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.garuda.data.local.dao.ContactDao
import com.example.garuda.data.local.entity.TrustedContactEntity

@Database(entities = [TrustedContactEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
