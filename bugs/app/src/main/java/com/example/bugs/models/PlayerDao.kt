package com.example.bugs.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPlayer(player: Player)

    @Update
    suspend fun updatePlayer(player: Player)

    @Query("SELECT * FROM players")
    suspend fun getAllPlayers(): List<Player>

    @Query("SELECT * FROM players WHERE name = :playerName LIMIT 1")
    suspend fun getPlayerByName(playerName: String): Player?

    @Query("DELETE FROM players")
    suspend fun clearAllPlayers()
}