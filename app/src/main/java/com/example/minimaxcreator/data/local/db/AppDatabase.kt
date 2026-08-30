package com.example.minimaxcreator.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GenerationRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun generationRecordDao(): GenerationRecordDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "minimax_creator.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE generation_records ADD COLUMN platform TEXT NOT NULL DEFAULT 'MINIMAX'")
                db.execSQL("ALTER TABLE generation_records ADD COLUMN model TEXT NOT NULL DEFAULT 'image-01'")
                db.execSQL("ALTER TABLE generation_records ADD COLUMN topMode TEXT NOT NULL DEFAULT 'IMAGE'")
                db.execSQL("ALTER TABLE generation_records ADD COLUMN subMode TEXT NOT NULL DEFAULT 'TEXT_TO_IMAGE'")
            }
        }
    }
}
