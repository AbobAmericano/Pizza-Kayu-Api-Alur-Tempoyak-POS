package com.slicehouse.pos.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Staff accounts plus the sign-in counters kept in app_meta. */
@Dao
interface StaffDao {

    @Query("SELECT * FROM staff ORDER BY createdAt")
    suspend fun all(): List<StaffEntity>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun get(id: String): StaffEntity?

    @Upsert
    suspend fun upsert(staff: StaffEntity)

    @Query("UPDATE staff SET pinHash = :pinHash WHERE id = :id")
    suspend fun setPin(id: String, pinHash: String)

    @Query("UPDATE staff SET active = :active WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean)

    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Upsert
    suspend fun putMeta(meta: MetaEntity)

    companion object {
        const val KEY_FAILED_PINS = "failed_pin_attempts"
        const val KEY_LOCKED_UNTIL = "pin_locked_until"
        const val KEY_RECOVERY_HASH = "owner_recovery_hash"
        /** The owner the recovery code resets: the one created at setup. */
        const val KEY_RECOVERY_OWNER = "owner_recovery_staff_id"
    }
}
