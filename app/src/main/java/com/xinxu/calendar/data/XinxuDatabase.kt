package com.xinxu.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 本地库。整个应用**只有这一张表、只有这一个库**，所有数据都在设备上，不联网。
 *
 * `exportSchema = true`：Room 会把表结构导出成 JSON 放在 `app/schemas/`，
 * 那个目录要跟着代码一起提交。它是将来做迁移的依据 ——
 * 没有它，改表结构时只能用 `fallbackToDestructiveMigration()`，
 * 那等于把用户攒了几个月的记录一次性删光。一本记录"足迹"的应用，这是最坏的事故。
 *
 * **不提供 `fallbackToDestructiveMigration()`。** 宁可升级时炸在开发机上，
 * 也不要静默地把用户的记录清空。
 */
@Database(
    entities = [DayEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class XinxuDatabase : RoomDatabase() {

    abstract fun dayEntryDao(): DayEntryDao

    companion object {
        private const val DB_NAME = "xinxu.db"

        @Volatile
        private var instance: XinxuDatabase? = null

        /**
         * 进程内单例。Room 的 `databaseBuilder` 每次调用都会新建一个连接池，
         * 多建几个不仅浪费，还可能在并发写的时候互相看不见对方的事务。
         */
        fun get(context: Context): XinxuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, XinxuDatabase::class.java, DB_NAME)
                    .build()
                    .also { instance = it }
            }
    }
}
