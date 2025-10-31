package com.example.bugs.managers

import android.content.Context
import android.util.Log
import com.example.bugs.database.AppDatabase
import com.example.bugs.models.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PlayerManager {


    private lateinit var db: AppDatabase


    fun initialize(context: Context) {
        db = AppDatabase.getDatabase(context)
    }

    /**
     * Добавляет нового игрока в базу данных.
     * @param player Объект игрока для добавления.
     */
    fun addPlayer(player: Player) {
        CoroutineScope(Dispatchers.IO).launch {
            db.playerDao().addPlayer(player)
            val playerCount = db.playerDao().getAllPlayers().size
            Log.i("PlayerManager", "Игрок добавлен: ${player.name}. Всего игроков: $playerCount")
        }
    }

    /**
     * Возвращает неизменяемый список всех зарегистрированных игроков из базы данных.
     * This is a suspend function because it's a database read operation.
     * @return List<Player> список игроков.
     */
    suspend fun getPlayers(): List<Player> {
        return withContext(Dispatchers.IO) {
            db.playerDao().getAllPlayers()
        }
    }

    /**
     * Очищает список всех игроков в базе данных.
     */
    fun clearPlayers() {
        CoroutineScope(Dispatchers.IO).launch {
            db.playerDao().clearAllPlayers()
            Log.i("PlayerManager", "Список игроков очищен.")
        }
    }

    /**
     * Обновляет максимальный счет игрока, если новый счет больше предыдущего рекорда.
     * @param playerName Имя игрока, чей счет нужно обновить.
     * @param newScore Новый счет, полученный в игре.
     */
    fun updatePlayerHighScore(playerName: String, newScore: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val player = db.playerDao().getPlayerByName(playerName)
            if (player != null) {
                if (newScore > player.highScore) {
                    player.highScore = newScore
                    db.playerDao().updatePlayer(player)
                    Log.i("PlayerManager", "Новый рекорд для игрока ${player.name}: $newScore")
                } else {
                    Log.i("PlayerManager", "Счет игрока ${player.name} ($newScore) не превысил рекорд (${player.highScore})")
                }
            } else {
                Log.w("PlayerManager", "Попытка обновить счет для несуществующего игрока: $playerName")
            }
        }
    }
}