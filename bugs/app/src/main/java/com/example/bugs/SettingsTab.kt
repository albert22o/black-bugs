package com.example.bugs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsTab : Fragment() {

    private lateinit var seekBarSpeed: SeekBar
    private lateinit var textSpeedValue: TextView
    private lateinit var seekBarMaxCockroaches: SeekBar
    private lateinit var textMaxCockroachesValue: TextView
    private lateinit var seekBarBonusInterval: SeekBar
    private lateinit var textBonusIntervalValue: TextView
    private lateinit var seekBarRoundDuration: SeekBar
    private lateinit var textRoundDurationValue: TextView
    private lateinit var buttonSaveSettings: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings_tab, container, false)

        // Инициализация View
        initViews(view)

        // Установка слушателей
        setupListeners()

        // Загрузка сохраненных настроек
        loadSavedSettings()

        return view
    }

    private fun initViews(view: View) {
        seekBarSpeed = view.findViewById(R.id.seekBarSpeed)
        textSpeedValue = view.findViewById(R.id.textSpeedValue)
        seekBarMaxCockroaches = view.findViewById(R.id.seekBarMaxCockroaches)
        textMaxCockroachesValue = view.findViewById(R.id.textMaxCockroachesValue)
        seekBarBonusInterval = view.findViewById(R.id.seekBarBonusInterval)
        textBonusIntervalValue = view.findViewById(R.id.textBonusIntervalValue)
        seekBarRoundDuration = view.findViewById(R.id.seekBarRoundDuration)
        textRoundDurationValue = view.findViewById(R.id.textRoundDurationValue)
        buttonSaveSettings = view.findViewById(R.id.buttonSaveSettings)
    }

    private fun setupListeners() {
        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSpeedText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarMaxCockroaches.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateMaxCockroachesText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarBonusInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateBonusIntervalText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarRoundDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateRoundDurationText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        buttonSaveSettings.setOnClickListener {
            saveSettings()
            Toast.makeText(requireContext(), "Настройки сохранены!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSpeedText(progress: Int) {
        val speedText = when (progress) {
            0 -> "Очень медленно (1)"
            1, 2 -> "Медленно ($progress)"
            3, 4, 5, 6, 7 -> "Средняя ($progress)"
            8, 9 -> "Быстро ($progress)"
            10 -> "Очень быстро (10)"
            else -> "Средняя ($progress)"
        }
        textSpeedValue.text = speedText
    }

    private fun updateMaxCockroachesText(progress: Int) {
        textMaxCockroachesValue.text = "$progress тараканов"
    }

    private fun updateBonusIntervalText(progress: Int) {
        textBonusIntervalValue.text = "$progress секунд"
    }

    private fun updateRoundDurationText(progress: Int) {
        val minutes = progress / 60
        val seconds = progress % 60
        val durationText = if (minutes > 0) {
            if (seconds > 0) "$minutes мин $seconds сек" else "$minutes минут"
        } else {
            "$progress секунд"
        }
        textRoundDurationValue.text = durationText
    }

    private fun loadSavedSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("GameSettings", 0)

        val speed = sharedPreferences.getInt("game_speed", 5)
        val maxCockroaches = sharedPreferences.getInt("max_cockroaches", 10)
        val bonusInterval = sharedPreferences.getInt("bonus_interval", 15)
        val roundDuration = sharedPreferences.getInt("round_duration", 120)

        seekBarSpeed.progress = speed
        seekBarMaxCockroaches.progress = maxCockroaches
        seekBarBonusInterval.progress = bonusInterval
        seekBarRoundDuration.progress = roundDuration

        updateSpeedText(speed)
        updateMaxCockroachesText(maxCockroaches)
        updateBonusIntervalText(bonusInterval)
        updateRoundDurationText(roundDuration)
    }

    private fun saveSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("GameSettings", 0)
        val editor = sharedPreferences.edit()

        editor.putInt("game_speed", seekBarSpeed.progress)
        editor.putInt("max_cockroaches", seekBarMaxCockroaches.progress)
        editor.putInt("bonus_interval", seekBarBonusInterval.progress)
        editor.putInt("round_duration", seekBarRoundDuration.progress)

        editor.apply()
    }

    companion object {
        fun getGameSpeed(context: android.content.Context): Int {
            val sharedPreferences = context.getSharedPreferences("GameSettings", 0)
            return sharedPreferences.getInt("game_speed", 5)
        }

        fun getMaxCockroaches(context: android.content.Context): Int {
            val sharedPreferences = context.getSharedPreferences("GameSettings", 0)
            return sharedPreferences.getInt("max_cockroaches", 10)
        }

        fun getBonusInterval(context: android.content.Context): Int {
            val sharedPreferences = context.getSharedPreferences("GameSettings", 0)
            return sharedPreferences.getInt("bonus_interval", 15)
        }

        fun getRoundDuration(context: android.content.Context): Int {
            val sharedPreferences = context.getSharedPreferences("GameSettings", 0)
            return sharedPreferences.getInt("round_duration", 120)
        }
    }
}