package com.example.bugs.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Класс данных для хранения информации об игроке.
 * @param id Уникальный идентификатор игрока.
 * @param name ФИО игрока.
 * @param gender Пол.
 * @param course Курс обучения.
 * @param birthDateMillis Дата рождения в миллисекундах.
 * @param zodiac Знак зодиака.
 */

@Parcelize
@Entity(tableName = "players") // Marks this class as a database table
data class Player(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // Marks this field as the primary key
    val name: String,
    val gender: String,
    val course: String,
    val birthDateMillis: Long,
    val zodiac: String,
    var highScore: Int = 0
): Parcelable