package com.slicehouse.pos.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version history (each version's layout is saved in app/schemas/):
 * 1. Menu, toppings, settings, orders (v1.5).
 * 2. Staff accounts and who took / refunded each order.
 *
 * Upgrades run automatically and keep every existing row.
 */
@Database(
    entities = [
        MenuItemEntity::class,
        ToppingEntity::class,
        ShopSettingsEntity::class,
        OrderEntity::class,
        OrderLineEntity::class,
        MetaEntity::class,
        StaffEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class PosDatabase : RoomDatabase() {
    abstract fun dao(): PosDao
    abstract fun staffDao(): StaffDao

    companion object {
        const val FILE_NAME = "pizza_pos.db"

        fun open(context: Context): PosDatabase =
            Room.databaseBuilder(context.applicationContext, PosDatabase::class.java, FILE_NAME)
                .build()
    }
}
