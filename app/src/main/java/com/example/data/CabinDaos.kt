package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CabinDao {
    @Query("SELECT * FROM cabins ORDER BY createdAt DESC")
    fun getAllCabins(): Flow<List<Cabin>>

    @Query("SELECT * FROM cabins")
    suspend fun getAllCabinsList(): List<Cabin>

    @Query("SELECT * FROM cabins WHERE id = :id")
    suspend fun getCabinById(id: String): Cabin?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCabin(cabin: Cabin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCabins(cabins: List<Cabin>)

    @Update
    suspend fun updateCabin(cabin: Cabin)

    @Query("DELETE FROM cabins WHERE id = :id")
    suspend fun deleteCabin(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM cabin_messages WHERE cabinId = :cabinId ORDER BY id ASC")
    fun getMessagesForCabin(cabinId: String): Flow<List<CabinMessage>>

    @Query("SELECT * FROM cabin_messages")
    suspend fun getAllMessagesList(): List<CabinMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CabinMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CabinMessage>)

    @Update
    suspend fun updateMessage(message: CabinMessage)

    @Query("SELECT * FROM cabin_messages WHERE id = :id")
    suspend fun getMessageById(id: String): CabinMessage?
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM cabin_tasks WHERE cabinId = :cabinId ORDER BY createdAt DESC")
    fun getTasksForCabin(cabinId: String): Flow<List<CabinTask>>

    @Query("SELECT * FROM cabin_tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<CabinTask>>

    @Query("SELECT * FROM cabin_tasks")
    suspend fun getAllTasksList(): List<CabinTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CabinTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<CabinTask>)

    @Update
    suspend fun updateTask(task: CabinTask)

    @Query("DELETE FROM cabin_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox_entries WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingEntriesFlow(): Flow<List<OutboxEntry>>

    @Query("SELECT * FROM outbox_entries WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingEntries(): List<OutboxEntry>

    @Query("SELECT COUNT(*) FROM outbox_entries WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox_entries WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEntry(entry: OutboxEntry)

    @Update
    suspend fun updateOutboxEntry(entry: OutboxEntry)

    @Query("DELETE FROM outbox_entries WHERE id = :id")
    suspend fun deleteOutboxEntry(id: String)

    @Query("DELETE FROM outbox_entries WHERE status = 'SYNCED'")
    suspend fun clearSyncedEntries()
}

