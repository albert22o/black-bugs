package com.example.bugs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.bugs.managers.PlayerManager
import com.example.bugs.models.Player
import java.text.SimpleDateFormat
import java.util.*

class RegistrationTab : Fragment() {

    private lateinit var editTextName: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var spinnerCourse: Spinner
    private lateinit var calendarViewBirth: CalendarView
    private lateinit var buttonRegister: Button
    private lateinit var textViewResult: TextView
    private lateinit var imageViewZodiac: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tab_registration, container, false)

        // Инициализация view
        editTextName = view.findViewById(R.id.editTextName)
        radioGroupGender = view.findViewById(R.id.radioGroupGender)
        spinnerCourse = view.findViewById(R.id.spinnerCourse)
        calendarViewBirth = view.findViewById(R.id.calendarViewBirth)
        buttonRegister = view.findViewById(R.id.buttonRegister)
        textViewResult = view.findViewById(R.id.textViewResult)
        imageViewZodiac = view.findViewById(R.id.imageViewZodiac)

        val courses = arrayOf("1 курс", "2 курс", "3 курс", "4 курс")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourse.adapter = adapter


        var birthDateMillis: Long = calendarViewBirth.date

        calendarViewBirth.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            birthDateMillis = cal.timeInMillis
            val zodiac = getZodiac(birthDateMillis)
            val zodiacResId = getZodiacImageRes(zodiac)
            if (zodiacResId != null) {
                imageViewZodiac.setImageResource(zodiacResId)
            } else {
                imageViewZodiac.setImageResource(android.R.color.transparent)
            }
        }

        val initialZodiac = getZodiac(birthDateMillis)
        getZodiacImageRes(initialZodiac)?.let { imageViewZodiac.setImageResource(it) }

        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            if (name.isEmpty()) {
                editTextName.error = "Введите ФИО"
                editTextName.requestFocus()
                return@setOnClickListener
            }

            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val gender = if (selectedGenderId != -1)
                view.findViewById<RadioButton>(selectedGenderId).text.toString()
            else "Не указан"

            val course = spinnerCourse.selectedItem.toString()

            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val birthDateStr = sdf.format(Date(birthDateMillis))
            val zodiac = getZodiac(birthDateMillis)

            val newPlayer = Player(
                name = name,
                gender = gender,
                course = course,
                birthDateMillis = birthDateMillis,
                zodiac = zodiac
            )

            // 2. Добавляем игрока через менеджер
            PlayerManager.addPlayer(newPlayer)

            // 3. Показываем пользователю уведомление об успехе
            Toast.makeText(requireContext(), "Игрок $name зарегистрирован!", Toast.LENGTH_SHORT).show()
            // --- КОНЕЦ ИНТЕГРАЦИИ ---


            // Отображение результата на экране (это можно оставить или убрать)
            textViewResult.text = """
                ФИО: $name
                Пол: $gender
                Курс: $course
                Дата рождения: $birthDateStr
                Знак зодиака: $zodiac
            """.trimIndent()

            getZodiacImageRes(zodiac)?.let { imageViewZodiac.setImageResource(it) }

            // Опционально: очистить форму после регистрации
            // clearForm()
        }

        return view
    }

    private fun clearForm() {
        editTextName.text.clear()
        radioGroupGender.clearCheck()
        spinnerCourse.setSelection(0)
        textViewResult.text = ""
        // calendarView можно сбросить на текущую дату, если нужно
        calendarViewBirth.date = System.currentTimeMillis()
    }

    private fun getZodiac(birthMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = birthMillis
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1 // Месяцы в Calendar с 0

        return when {
            (month == 3 && day >= 21) || (month == 4 && day <= 19) -> "Овен"
            (month == 4 && day >= 20) || (month == 5 && day <= 20) -> "Телец"
            (month == 5 && day >= 21) || (month == 6 && day <= 20) -> "Близнецы"
            (month == 6 && day >= 21) || (month == 7 && day <= 22) -> "Рак"
            (month == 7 && day >= 23) || (month == 8 && day <= 22) -> "Лев"
            (month == 8 && day >= 23) || (month == 9 && day <= 22) -> "Дева"
            (month == 9 && day >= 23) || (month == 10 && day <= 22) -> "Весы"
            (month == 10 && day >= 23) || (month == 11 && day <= 21) -> "Скорпион"
            (month == 11 && day >= 22) || (month == 12 && day <= 21) -> "Стрелец"
            (month == 12 && day >= 22) || (month == 1 && day <= 19) -> "Козерог"
            (month == 1 && day >= 20) || (month == 2 && day <= 18) -> "Водолей"
            (month == 2 && day >= 19) || (month == 3 && day <= 20) -> "Рыбы"
            else -> "Неизвестно"
        }
    }

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