package com.example.bugs

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
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

class GameActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_PLAYER = "EXTRA_PLAYER"
        const val EXTRA_GAME_SPEED = "EXTRA_GAME_SPEED"
        const val EXTRA_MAX_COCKROACHES = "EXTRA_MAX_COCKROACHES"
        const val EXTRA_BONUS_INTERVAL = "EXTRA_BONUS_INTERVAL"
        const val EXTRA_ROUND_DURATION = "EXTRA_ROUND_DURATION"

        // НОВОЕ: Константы для бонуса
        private const val BONUS_LIFETIME_MS = 1000L // 10 секунд действия бонуса
        private const val TILT_SENSITIVITY = 4.5f // Чувствительность к наклону
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
    private var bonusInterval = 15000L

    // Игровое состояние
    private var score = 0
    private var currentPlayer: Player? = null
    private val cockroaches = mutableListOf<ImageView>()
    private val gameHandler = Handler(Looper.getMainLooper())
    private var isGameRunning = false
    private lateinit var gameTimer: CountDownTimer

    // НОВОЕ: Для бонуса и сенсоров
    private var bonusView: ImageView? = null
    private var isBonusActive = false
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var soundPool: SoundPool? = null
    private var bugScreamSoundId: Int = 0

    // НОВОЕ: Размеры игровой области для сенсоров
    private var gameAreaWidth = 0
    private var gameAreaHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Инициализация UI
        scoreTextView = findViewById(R.id.textViewScore)
        timerTextView = findViewById(R.id.textViewTimer)
        playerNameTextView = findViewById(R.id.textViewPlayerName)
        gameArea = findViewById(R.id.gameArea)


        currentPlayer = getPlayerFromIntent()
        gameSpeed = intent.getIntExtra(EXTRA_GAME_SPEED, 5)
        maxCockroaches = intent.getIntExtra(EXTRA_MAX_COCKROACHES, 10)
        roundDuration = intent.getIntExtra(EXTRA_ROUND_DURATION, 120).toLong()
        bonusInterval = intent.getIntExtra(EXTRA_BONUS_INTERVAL, 15).toLong() * 1000

        if (currentPlayer == null) {
            Toast.makeText(this, "Ошибка: данные игрока не найдены", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        playerNameTextView.text = "Игрок: ${currentPlayer!!.name}"

        // Слушатель для промахов
        gameArea.setOnClickListener {
            if (isGameRunning) {
                updateScore(-1)
            }
        }

        setupSensors()
        setupSoundPool()

        gameArea.post {
            gameAreaWidth = gameArea.width
            gameAreaHeight = gameArea.height
            startGame()
        }
    }

    private fun startGame() {
        if (gameAreaWidth == 0 || gameAreaHeight == 0) {
            // Если размеры еще не получены, попробуем еще раз через мгновение
            gameArea.post { startGame() }
            return
        }
        isGameRunning = true
        score = 0
        updateScore(0)
        startRoundTimer()
        gameHandler.post(spawner) // Запускаем спавн тараканов
        gameHandler.postDelayed(bonusSpawner, bonusInterval)
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
            val spawnInterval = 1000L / gameSpeed
            gameHandler.postDelayed(this, spawnInterval)
        }
    }

    // НОВОЕ: Игровой цикл для спавна бонусов
    private val bonusSpawner = object : Runnable {
        override fun run() {
            if (isGameRunning && bonusView == null && !isBonusActive) {
                spawnBonus()
            }
            // Планируем следующий запуск
            gameHandler.postDelayed(this, bonusInterval)
        }
    }

    private fun spawnCockroach() {
        val cockroachView = ImageView(this)
        cockroachView.setImageResource(R.drawable.cockroach)
        val size = (80 + Random.nextInt(0, 50)).dpToPx()
        val params = FrameLayout.LayoutParams(size, size)
        cockroachView.layoutParams = params

        // Размеры уже должны быть известны к этому моменту
        val maxX = gameAreaWidth - size
        val maxY = gameAreaHeight - size

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
            // Запускаем движение, если бонус не активен
            if (!isBonusActive) {
                moveCockroach(cockroachView, maxX, maxY)
            }
        }
    }

    // НОВОЕ: Спавн бонуса
    private fun spawnBonus() {
        bonusView = ImageView(this).apply {
            setImageResource(R.drawable.ic_bonus)
            val size = 100.dpToPx()
            layoutParams = FrameLayout.LayoutParams(size, size)

            val maxX = gameAreaWidth - size
            val maxY = gameAreaHeight - size

            if (maxX > 0 && maxY > 0) {
                x = Random.nextInt(0, maxX).toFloat()
                y = Random.nextInt(0, maxY).toFloat()

                setOnClickListener {
                    if (isGameRunning) {
                        activateBonusEffect()
                        gameArea.removeView(this)
                        bonusView = null
                    }
                }
            }
        }
        gameArea.addView(bonusView)
    }

    private fun moveCockroach(cockroachView: ImageView, maxX: Int, maxY: Int) {

        if (!isGameRunning || !cockroaches.contains(cockroachView) || isBonusActive) return

        val newX = Random.nextInt(0, maxX).toFloat()
        val newY = Random.nextInt(0, maxY).toFloat()
        val moveDuration = 2000L - (gameSpeed * 150)

        cockroachView.animate()
            .x(newX)
            .y(newY)
            .setDuration(moveDuration.coerceAtLeast(500L))
            .withEndAction {

                if (!isBonusActive) {
                    moveCockroach(cockroachView, maxX, maxY)
                }
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
        gameHandler.removeCallbacksAndMessages(null)

        if (isBonusActive) {
            stopBonusEffect()
        }
        bonusView?.let {
            gameArea.removeView(it)
            bonusView = null
        }

        currentPlayer?.let { player ->
            PlayerManager.updatePlayerHighScore(player.name, score)
        }
        cockroaches.forEach { gameArea.removeView(it) }
        cockroaches.clear()

        AlertDialog.Builder(this)
            .setTitle("Раунд окончен!")
            .setMessage("Ваш итоговый счет: $score")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::gameTimer.isInitialized) {
            gameTimer.cancel()
        }
        gameHandler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        soundPool?.release()
        soundPool = null
    }


    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Toast.makeText(this, "Акселерометр не найден!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        bugScreamSoundId = soundPool?.load(this, R.raw.bug_scream, 1) ?: 0
    }

    private fun activateBonusEffect() {
        if (accelerometer == null) return
        isBonusActive = true

        soundPool?.play(bugScreamSoundId, 1.0f, 1.0f, 1, 0, 1.0f)

        cockroaches.forEach { it.animate().cancel() }

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        gameHandler.postDelayed({ stopBonusEffect() }, BONUS_LIFETIME_MS)
    }

    private fun stopBonusEffect() {
        isBonusActive = false
        sensorManager.unregisterListener(this)

        val maxX = gameAreaWidth - 130.dpToPx()
        val maxY = gameAreaHeight - 130.dpToPx()
        if (maxX > 0 && maxY > 0) {
            cockroaches.forEach { moveCockroach(it, maxX, maxY) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Вызывается постоянно, но мы реагируем только если бонус активен
        if (!isBonusActive || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        val gravityX = event.values[0]
        val gravityY = event.values[1]

        cockroaches.forEach { cockroach ->
            val bugSize = cockroach.width.toFloat() // Используем реальный размер

            var newX = cockroach.x - (gravityX * TILT_SENSITIVITY)

            var newY = cockroach.y + (gravityY * TILT_SENSITIVITY)

            // Ограничиваем движение границами
            newX = newX.coerceIn(0f, gameAreaWidth - bugSize)
            newY = newY.coerceIn(0f, gameAreaHeight - bugSize)

            // Применяем новые координаты
            cockroach.x = newX
            cockroach.y = newY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Не используется, но обязателен для реализации
    }


    private fun getPlayerFromIntent(): Player? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PLAYER, Player::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PLAYER)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}