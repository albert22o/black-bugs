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

            // Запускаем корутину для загрузки данных
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Упрощенный парсинг XML вручную для надежности в виджете,
                    // так как SimpleXML может конфликтовать внутри Receiver контекста
                    val date = RetrofitClient.getCurrentDate()
                    val urlString = "https://www.cbr.ru/scripts/xml_metall.asp?date_req1=$date&date_req2=$date"
                    val url = URL(urlString)
                    val connection = url.openConnection()
                    val reader = BufferedReader(InputStreamReader(connection.getInputStream(), "windows-1251"))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    reader.close()

                    val xml = sb.toString()
                    // Ищем код золота "1" и цену Buy
                    // Это "грязный" хак парсинга, но он работает без тяжелых библиотек в виджете
                    val codeIndex = xml.indexOf("Code=\"1\"")
                    var price = "Err"
                    if (codeIndex != -1) {
                        val buyTag = "<Buy>"
                        val start = xml.indexOf(buyTag, codeIndex) + buyTag.length
                        val end = xml.indexOf("</Buy>", start)
                        if (start > 0 && end > 0) {
                            price = xml.substring(start, end)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.widget_price_text, "$price ₽")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}