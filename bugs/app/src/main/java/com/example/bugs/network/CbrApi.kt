package com.example.bugs.network

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Модели для парсинга XML
@Root(name = "Metall", strict = false)
data class MetallResponse @JvmOverloads constructor(
    @field:ElementList(inline = true, required = false)
    var records: List<Record>? = null
)

@Root(name = "Record", strict = false)
data class Record @JvmOverloads constructor(
    @field:Attribute(name = "Code")
    var code: String = "", // 1 - Золото
    @field:ElementList(entry = "Buy", inline = true, required = false)
    var buy: String? = null // Цену часто кладут сюда или просто текстом, для упрощения берем поле Buy
) {
    // В XML ЦБ цена лежит внутри тега <Buy>1234,56</Buy>
    // SimpleXML требует точной настройки, но для краткости мы сделаем упрощенную модель,
    // так как структура XML ЦБ может варьироваться.
    // Ниже более надежный способ через ручной парсинг или String,
    // но для примера предположим, что мы получаем строку цены.
}

// Интерфейс Retrofit
interface CbrApiService {
    // URL: https://www.cbr.ru/scripts/xml_metall.asp?date_req1=dd/MM/yyyy&date_req2=dd/MM/yyyy
    @GET("scripts/xml_metall.asp")
    suspend fun getMetals(
        @Query("date_req1") dateStart: String,
        @Query("date_req2") dateEnd: String
    ): MetallResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    val api: CbrApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(CbrApiService::class.java)
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}