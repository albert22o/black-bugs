package com.example.bugs

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.bugs.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class GoldWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_gold)
            views.setTextViewText(R.id.widget_price_text, "Загрузка...")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Загрузка в фоне
            CoroutineScope(Dispatchers.IO).launch {
                var priceDisplay = "Нет сети"
                try {
                    // Запрашиваем за неделю, чтобы исключить выходные
                    val dateStart = RetrofitClient.getWeekAgoDate()
                    val dateEnd = RetrofitClient.getTodayDate()

                    val urlString = "https://www.cbr.ru/scripts/xml_metall.asp?date_req1=$dateStart&date_req2=$dateEnd"
                    val url = URL(urlString)

                    // Читаем ответ
                    val connection = url.openConnection()
                    connection.connectTimeout = 5000
                    val reader = BufferedReader(InputStreamReader(connection.getInputStream(), "windows-1251")) // ЦБ использует win-1251
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    reader.close()
                    val xml = sb.toString()

                    // Ручной парсинг: ищем последние данные по коду "1" (Золото)
                    // XML приходит в виде списка <Record Code="1" ...><Buy>5000,00</Buy></Record>...
                    // Нам нужно найти последнее вхождение Code="1"

                    val codePattern = "Code=\"1\""
                    val lastIndex = xml.lastIndexOf(codePattern) // Берем самую последнюю запись (свежую дату)

                    if (lastIndex != -1) {
                        val buyTag = "<Buy>"
                        val start = xml.indexOf(buyTag, lastIndex) + buyTag.length
                        val end = xml.indexOf("</Buy>", start)
                        if (start > 0 && end > 0) {
                            val rawPrice = xml.substring(start, end)
                            // Форматируем: 5432,12 -> 5432 ₽
                            val cleanPrice = rawPrice.split(",")[0]
                            priceDisplay = "$cleanPrice ₽"
                        }
                    } else {
                        priceDisplay = "ЦБ недоступен"
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    priceDisplay = "Ошибка"
                }

                // Обновляем UI
                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_price_text, priceDisplay)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}