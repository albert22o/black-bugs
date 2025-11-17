package com.example.bugs.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugs.repository.GoldRepository
import kotlinx.coroutines.launch

class GameViewModel(private val repository: GoldRepository) : ViewModel() {

    // LiveData для наблюдения из Activity
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> get() = _score

    private val _timeLeft = MutableLiveData<Long>(0)
    val timeLeft: LiveData<Long> get() = _timeLeft

    private val _isGameOver = MutableLiveData(false)
    val isGameOver: LiveData<Boolean> get() = _isGameOver

    // Внутреннее состояние
    var currentGoldPrice: Double = 5000.0
        private set

    var isGameRunning = false
        private set

    private var timer: CountDownTimer? = null

    init {
        // Загружаем золото сразу при создании ViewModel
        viewModelScope.launch {
            currentGoldPrice = repository.getGoldPrice()
        }
    }

    fun startGame(durationSec: Long) {
        if (isGameRunning) return // Если игра уже идет (например, после поворота), не сбрасываем

        isGameRunning = true
        _score.value = 0
        _isGameOver.value = false

        startTimer(durationSec * 1000)
    }

    private fun startTimer(millis: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = millisUntilFinished / 1000
            }

            override fun onFinish() {
                _timeLeft.value = 0
                endGame()
            }
        }.start()
    }

    fun addScore(points: Int) {
        val current = _score.value ?: 0
        _score.value = (current + points).coerceAtLeast(0)
    }

    fun endGame() {
        isGameRunning = false
        timer?.cancel()
        _isGameOver.value = true
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}