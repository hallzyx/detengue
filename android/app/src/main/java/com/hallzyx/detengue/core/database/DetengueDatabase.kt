package com.hallzyx.detengue.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [InspectionEntity::class, DetectionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class DetengueDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao

    companion object {
        fun create(context: Context): DetengueDatabase =
            Room.databaseBuilder(context.applicationContext, DetengueDatabase::class.java, "detengue.db").build()
    }
}
