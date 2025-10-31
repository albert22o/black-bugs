package com.example.bugs

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bugs.managers.PlayerManager
import com.example.bugs.models.Player
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER = "EXTRA_PLAYER"
        const val EXTRA_GAME_SPEED = "EXTRA_GAME_SPEED"
        const val EXTRA_MAX_COCKROACHES = "EXTRA_MAX_COCKROACHES"
        const val EXTRA_BONUS_INTERVAL = "EXTRA_BONUS_INTERVAL" // Пока не используется, но можно добавить логику
        const val EXTRA_ROUND_DURATION = "EXTRA_ROUND_DURATION"
    }

    // UI элементы
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var playerNameTextView: TextView
    private lateinit var gameArea: FrameLayout

    // Параметры игры
    private var gameSpeed = 0
    private var maxCockroaches = 0
    private var roundDuration = 0L

    // Игровое состояние
    private var score = 0
    private var currentPlayer: Player? = null
    private val cockroaches = mutableListOf<ImageView>()
    private val gameHandler = Handler(Looper.getMainLooper())
    private var isGameRunning = false
    private lateinit var gameTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        currentPlayer = getPlayerFromIntent()
        if (currentPlayer == null) {
            Toast.makeText(this, "Ошибка: данные игрока не найдены", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // Получаем данные из Intent
        val player = getPlayerFromIntent()
        gameSpeed = intent.getIntExtra(EXTRA_GAME_SPEED, 5)
        maxCockroaches = intent.getIntExtra(EXTRA_MAX_COCKROACHES, 10)
        roundDuration = intent.getIntExtra(EXTRA_ROUND_DURATION, 120).toLong()
        // val bonusInterval = intent.getIntExtra(EXTRA_BONUS_INTERVAL, 15) // Можно будет использовать для бонусов

        if (player == null) {
            Toast.makeText(this, "Ошибка: данные игрока не найдены", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Инициализация UI
        scoreTextView = findViewById(R.id.textViewScore)
        timerTextView = findViewById(R.id.textViewTimer)
        playerNameTextView = findViewById(R.id.textViewPlayerName)
        gameArea = findViewById(R.id.gameArea)

        playerNameTextView.text = "Игрок: ${player.name}"

        // Слушатель для промахов
        gameArea.setOnClickListener {
            if (isGameRunning) {
                updateScore(-1)
            }
        }

        startGame()
    }

    private fun startGame() {
        isGameRunning = true
        score = 0
        updateScore(0) // Инициализируем текст счета
        startRoundTimer()
        gameHandler.post(spawner) // Запускаем спавн тараканов
    }

    private fun startRoundTimer() {
        gameTimer = object : CountDownTimer(roundDuration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerTextView.text = "Время: $secondsLeft"
            }

            override fun onFinish() {
                timerTextView.text = "Время: 0"
                endGame()
            }
        }.start()
    }

    // Игровой цикл для спавна тараканов
    private val spawner = object : Runnable {
        override fun run() {
            if (isGameRunning && cockroaches.size < maxCockroaches) {
                spawnCockroach()
            }
            // Чем выше скорость, тем чаще спавн
            val spawnInterval = 1000L / gameSpeed
            gameHandler.postDelayed(this, spawnInterval)
        }
    }

    private fun spawnCockroach() {
        val cockroachView = ImageView(this)
        cockroachView.setImageResource(R.drawable.cockroach)
        // Устанавливаем размер таракана
        val size = (80 + Random.nextInt(0, 50)).dpToPx()
        val params = FrameLayout.LayoutParams(size, size)
        cockroachView.layoutParams = params

        // Размещаем в случайном месте, гарантируя, что он полностью внутри gameArea
        gameArea.post { // Ждем, пока gameArea не будет отрисована, чтобы получить ее размеры
            val maxX = gameArea.width - size
            val maxY = gameArea.height - size
            if (maxX > 0 && maxY > 0) {
                cockroachView.x = Random.nextInt(0, maxX).toFloat()
                cockroachView.y = Random.nextInt(0, maxY).toFloat()

                gameArea.addView(cockroachView)
                cockroaches.add(cockroachView)

                cockroachView.setOnClickListener {
                    if (isGameRunning) {
                        updateScore(2)
                        gameArea.removeView(it)
                        cockroaches.remove(it)
                    }
                }
                // Запускаем движение для нового таракана
                moveCockroach(cockroachView, maxX, maxY)
            }
        }
    }

    private fun moveCockroach(cockroachView: ImageView, maxX: Int, maxY: Int) {
        if (!isGameRunning || !cockroaches.contains(cockroachView)) return

        val newX = Random.nextInt(0, maxX).toFloat()
        val newY = Random.nextInt(0, maxY).toFloat()

        // Длительность анимации зависит от скорости игры
        val moveDuration = 2000L - (gameSpeed * 150)

        cockroachView.animate()
            .x(newX)
            .y(newY)
            .setDuration(moveDuration.coerceAtLeast(500L)) // Минимальная длительность 500мс
            .withEndAction {
                // После завершения анимации запускаем новую
                moveCockroach(cockroachView, maxX, maxY)
            }
            .start()
    }

    private fun updateScore(change: Int) {
        score += change
        if (score < 0) {
            score = 0
        }
        scoreTextView.text = "Счет: $score"
    }

    private fun endGame() {
        isGameRunning = false
        gameTimer.cancel()
        gameHandler.removeCallbacksAndMessages(null) // Останавливаем все циклы
        currentPlayer?.let { player ->
            PlayerManager.updatePlayerHighScore(player.name, score)
        }
        // Удаляем оставшихся тараканов
        cockroaches.forEach { gameArea.removeView(it) }
        cockroaches.clear()

        // Показываем диалог с результатами
        AlertDialog.Builder(this)
            .setTitle("Раунд окончен!")
            .setMessage("Ваш итоговый счет: $score")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Убедимся, что все остановлено, чтобы избежать утечек памяти
        if (::gameTimer.isInitialized) {
            gameTimer.cancel()
        }
        gameHandler.removeCallbacksAndMessages(null)
    }

    private fun getPlayerFromIntent(): Player? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PLAYER, Player::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PLAYER)
        }
    }

    // Вспомогательная функция для конвертации dp в пиксели
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}