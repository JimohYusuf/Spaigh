package com.example.spaigh.database

import androidx.room.*

/*
    Functions of this class are suspend functions, meaning that they run on a background
    thread different from the main UI thread. This is important for database management
    operations.
 */
@Dao
interface DataDao {
    @Query("SELECT * FROM data")
    suspend fun getAll(): List<Data>

    @Query("SELECT * FROM data WHERE timeStamp IN (:timestamps)")
    suspend fun loadAllByIds(timestamps: IntArray): List<Data>

    @Query("SELECT * FROM data WHERE timeStamp LIKE :Time")
    suspend fun findByTimeStamp(Time: String): Data

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg data: Data)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg data: Data)

    @Delete
    suspend fun delete(data: Data)

    @Query("DELETE FROM data")
    suspend fun deleteAllData()
}
