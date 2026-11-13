package com.slicehouse.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MenuItemEntity::class,
        ToppingEntity::class,
        ShopSettingsEntity::class,
        OrderEntity::class,
        OrderLineEntity::class,
        MetaEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PosDatabase : RoomDatabase() {
    abstract fun dao(): PosDao

    companion object {
        const val FILE_NAME = "pizza_pos.db"

        fun open(context: Context): PosDatabase =
            Room.databaseBuilder(context.applicationContext, PosDatabase::class.java, FILE_NAME)
                .build()
    }
}
