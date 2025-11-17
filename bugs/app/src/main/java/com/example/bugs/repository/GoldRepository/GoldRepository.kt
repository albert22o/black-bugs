package com.example.bugs.repository

import com.example.bugs.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoldRepository {
    suspend fun getGoldPrice(): Double {
        return withContext(Dispatchers.IO) {
            try {
                val dateStart = RetrofitClient.getWeekAgoDate()
                val dateEnd = RetrofitClient.getTodayDate()
                val response = RetrofitClient.api.getMetals(dateStart, dateEnd)

                val goldRecords = response.records?.filter { it.code == "1" }
                if (!goldRecords.isNullOrEmpty()) {
                    // Берем последнюю запись и меняем запятую на точку
                    val lastRecord = goldRecords.last()
                    lastRecord.buy.replace(",", ".").toDoubleOrNull() ?: 5000.0
                } else {
                    5000.0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                5000.0 // Фолбэк цена
            }
        }
    }
}