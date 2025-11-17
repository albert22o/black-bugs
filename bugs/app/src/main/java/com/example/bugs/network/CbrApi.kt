package com.example.bugs.network

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- МОДЕЛИ ДАННЫХ ---

@Root(name = "Metall", strict = false)
data class MetallResponse @JvmOverloads constructor(
    @field:ElementList(inline = true, required = false)
    var records: List<Record>? = null
)

@Root(name = "Record", strict = false)
data class Record @JvmOverloads constructor(
    @field:Attribute(name = "Code")
    var code: String = "", // "1" = Золото
    @field:Attribute(name = "Date")
    var date: String = "",
    @field:Element(name = "Buy")
    var buy: String = "" // Цена покупки (используем её как курс)
)

// --- ИНТЕРФЕЙС RETROFIT ---

interface CbrApiService {
    // Запрашиваем металлы за диапазон дат
    @GET("scripts/xml_metall.asp")
    suspend fun getMetals(
        @Query("date_req1") dateStart: String,
        @Query("date_req2") dateEnd: String
    ): MetallResponse
}

// --- КЛИЕНТ ---

object RetrofitClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    val api: CbrApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(CbrApiService::class.java)
    }

    // Получаем дату "сегодня"
    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    // Получаем дату "7 дней назад" (чтобы точно захватить последний рабочий день)
    fun getWeekAgoDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}