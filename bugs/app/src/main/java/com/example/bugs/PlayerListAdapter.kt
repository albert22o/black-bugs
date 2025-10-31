package com.example.bugs.com.example.bugs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.bugs.R
import com.example.bugs.models.Player

class PlayerListAdapter(context: Context, private val players: List<Player>) :
    ArrayAdapter<Player>(context, 0, players) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Получаем или создаем View для элемента списка
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_player, parent, false)

        // Находим View внутри макета
        val playerName: TextView = view.findViewById(R.id.textViewPlayerName)
        val playerInfo: TextView = view.findViewById(R.id.textViewPlayerInfo)
        val playerZodiacImage: ImageView = view.findViewById(R.id.imageViewPlayerZodiac)
        val playerHighScore: TextView = view.findViewById(R.id.textViewPlayerHighScore)
        // Получаем текущего игрока
        val player = getItem(position)

        if (player != null) {
            // Заполняем View данными
            playerName.text = player.name
            playerInfo.text = "${player.course}, ${player.gender}"
            playerHighScore.text = "Лучший счет: ${player.highScore}"
            // Устанавливаем иконку знака зодиака
            val zodiacResId = getZodiacImageRes(player.zodiac)
            if (zodiacResId != null) {
                playerZodiacImage.setImageResource(zodiacResId)
            } else {
                // Можно установить картинку по умолчанию, если знака нет
                playerZodiacImage.setImageResource(R.mipmap.ic_launcher)
            }
        }

        return view
    }

    // Вспомогательная функция для получения ресурса изображения по названию знака
    private fun getZodiacImageRes(zodiac: String): Int? {
        return when (zodiac) {
            "Овен" -> R.drawable.aries
            "Телец" -> R.drawable.taurus
            "Близнецы" -> R.drawable.gemini
            "Рак" -> R.drawable.cancer
            "Лев" -> R.drawable.leo
            "Дева" -> R.drawable.virgo
            "Весы" -> R.drawable.libra
            "Скорпион" -> R.drawable.scorpio
            "Стрелец" -> R.drawable.sagittarius
            "Козерог" -> R.drawable.capricorn
            "Водолей" -> R.drawable.aquarius
            "Рыбы" -> R.drawable.pisces
            else -> null
        }
    }
}