package com.example.bugs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bugs.com.example.bugs.PlayerListAdapter
import com.example.bugs.managers.PlayerManager
import com.example.bugs.models.Player
import kotlinx.coroutines.launch

class PlayerListTab : Fragment() {

    private lateinit var listViewPlayers: ListView
    private lateinit var buttonPlay: Button
    private lateinit var textViewEmptyList: TextView

    private var selectedPlayer: Player? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_list, container, false)
        listViewPlayers = view.findViewById(R.id.listViewPlayers)
        buttonPlay = view.findViewById(R.id.buttonPlay)
        textViewEmptyList = view.findViewById(R.id.textViewEmptyList)
        setupListeners()
        return view
    }

    override fun onResume() {
        super.onResume()
        updatePlayerList()
    }
    private fun updatePlayerList() {

        viewLifecycleOwner.lifecycleScope.launch {

            val players = PlayerManager.getPlayers()

            if (players.isEmpty()) {
                textViewEmptyList.visibility = View.VISIBLE
                listViewPlayers.visibility = View.GONE
            } else {
                textViewEmptyList.visibility = View.GONE
                listViewPlayers.visibility = View.VISIBLE
                val adapter = PlayerListAdapter(requireContext(), players)
                listViewPlayers.adapter = adapter
            }

            selectedPlayer = null
            buttonPlay.visibility = View.GONE
            listViewPlayers.adapter?.let {
                listViewPlayers.setItemChecked(-1, true)
            }
        }
    }


    private fun setupListeners() {
        // Обработчик нажатия на элемент списка
        listViewPlayers.setOnItemClickListener { parent, view, position, id ->
            // Получаем выбранного игрока
            selectedPlayer = parent.getItemAtPosition(position) as Player
            // Показываем кнопку "Играть"
            buttonPlay.visibility = View.VISIBLE
        }

        // Обработчик нажатия на кнопку "Играть"
        buttonPlay.setOnClickListener {
            // Проверяем, что игрок выбран
            selectedPlayer?.let { player ->
                // Создаем Intent для запуска GameActivity
                val intent = Intent(activity, GameActivity::class.java).apply {
                    // 1. Кладем в Intent выбранного игрока
                    putExtra(GameActivity.EXTRA_PLAYER, player)

                    // 2. Получаем настройки из SharedPreferences через статические методы SettingsTab
                    val gameSpeed = SettingsTab.getGameSpeed(requireContext())
                    val maxCockroaches = SettingsTab.getMaxCockroaches(requireContext())
                    val bonusInterval = SettingsTab.getBonusInterval(requireContext())
                    val roundDuration = SettingsTab.getRoundDuration(requireContext())

                    // 3. Кладем настройки в Intent
                    putExtra(GameActivity.EXTRA_GAME_SPEED, gameSpeed)
                    putExtra(GameActivity.EXTRA_MAX_COCKROACHES, maxCockroaches)
                    putExtra(GameActivity.EXTRA_BONUS_INTERVAL, bonusInterval)
                    putExtra(GameActivity.EXTRA_ROUND_DURATION, roundDuration)
                }

                // Запускаем активность
                startActivity(intent)
            }
        }
    }
}