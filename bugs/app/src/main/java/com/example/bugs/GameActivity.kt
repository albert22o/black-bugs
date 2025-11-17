package com.example.bugs

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
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
import androidx.lifecycle.lifecycleScope
import com.example.bugs.managers.PlayerManager
import com.example.bugs.models.Player
import com.example.bugs.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GameActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_PLAYER = "EXTRA_PLAYER"
        const val EXTRA_GAME_SPEED = "EXTRA_GAME_SPEED"
        const val EXTRA_MAX_COCKROACHES = "EXTRA_MAX_COCKROACHES"
        const val EXTRA_BONUS_INTERVAL = "EXTRA_BONUS_INTERVAL"
        const val EXTRA_ROUND_DURATION = "EXTRA_ROUND_DURATION"

        private const val BONUS_LIFETIME_MS = 1000L
        private const val TILT_SENSITIVITY = 4.5f

        // НОВОЕ: Интервал золотого таракана
        private const val GOLD_BUG_INTERVAL = 20000L
    }

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var playerNameTextView: TextView
    private lateinit var gameArea: FrameLayout

    private var gameSpeed = 0
    private var maxCockroaches = 0
    private var roundDuration = 0L
    private var bonusInterval = 1000L

    private var score = 0
    private var currentPlayer: Player? = null
    private val cockroaches = mutableListOf<ImageView>()
    private val gameHandler = Handler(Looper.getMainLooper())
    private var isGameRunning = false
    private lateinit var gameTimer: CountDownTimer

    private var bonusView: ImageView? = null
    private var isBonusActive = false
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var soundPool: SoundPool? = null
    private var bugScreamSoundId: Int = 0

    private var gameAreaWidth = 0
    private var gameAreaHeight = 0

    // НОВОЕ: Переменная для курса золота
    private var currentGoldPrice: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

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

        gameArea.setOnClickListener {
            if (isGameRunning) {
                updateScore(-1)
            }
        }

        setupSensors()
        setupSoundPool()

        // НОВОЕ: Загружаем курс золота при старте
        fetchGoldPrice()

        gameArea.post {
            gameAreaWidth = gameArea.width
            gameAreaHeight = gameArea.height
            startGame()
        }
    }

    // НОВОЕ: Функция загрузки курса золота
    private fun fetchGoldPrice() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dateStart = RetrofitClient.getWeekAgoDate()
                val dateEnd = RetrofitClient.getTodayDate()

                // Используем Retrofit для запроса
                val response = RetrofitClient.api.getMetals(dateStart, dateEnd)

                // Фильтруем: ищем записи с кодом "1" (Золото)
                val goldRecords = response.records?.filter { it.code == "1" }

                if (!goldRecords.isNullOrEmpty()) {
                    // Берем последнюю запись (самую свежую дату)
                    val lastRecord = goldRecords.last()

                    // ЦБ возвращает цену с запятой (например "5432,56"), меняем на точку
                    val priceStr = lastRecord.buy.replace(",", ".")
                    currentGoldPrice = priceStr.toDoubleOrNull() ?: 5000.0

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@GameActivity, "Курс золота: ${lastRecord.buy}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Если список пуст (крайне маловероятно при запросе за неделю)
                    currentGoldPrice = 5000.0
                }

            } catch (e: Exception) {
                e.printStackTrace()
                currentGoldPrice = 5000.0 // Фолбэк значение при ошибке интернета
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GameActivity, "Ошибка загрузки курса", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startGame() {
        if (gameAreaWidth == 0 || gameAreaHeight == 0) {
            gameArea.post { startGame() }
            return
        }
        isGameRunning = true
        score = 0
        updateScore(0)
        startRoundTimer()
        gameHandler.post(spawner)
        gameHandler.postDelayed(bonusSpawner, bonusInterval)

        // НОВОЕ: Запуск спавнера золотого таракана
        gameHandler.postDelayed(goldBugSpawner, GOLD_BUG_INTERVAL)
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

    private val spawner = object : Runnable {
        override fun run() {
            if (isGameRunning && cockroaches.size < maxCockroaches) {
                spawnCockroach(isGold = false)
            }
            val spawnInterval = 1000L / gameSpeed
            gameHandler.postDelayed(this, spawnInterval)
        }
    }

    private val bonusSpawner = object : Runnable {
        override fun run() {
            if (isGameRunning && bonusView == null && !isBonusActive) {
                spawnBonus()
            }
            gameHandler.postDelayed(this, bonusInterval)
        }
    }

    // НОВОЕ: Спавнер золотого таракана
    private val goldBugSpawner = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                spawnCockroach(isGold = true)
            }
            gameHandler.postDelayed(this, GOLD_BUG_INTERVAL)
        }
    }

    // Изменили сигнатуру для поддержки золотых тараканов
    private fun spawnCockroach(isGold: Boolean) {
        val cockroachView = ImageView(this)
        cockroachView.setImageResource(R.drawable.cockroach)

        // НОВОЕ: Если золотой, красим его и увеличиваем
        if (isGold) {
            cockroachView.setColorFilter(Color.parseColor("#FFD700"), PorterDuff.Mode.SRC_IN)
        }

        val baseSize = if (isGold) 120 else 80
        val size = (baseSize + Random.nextInt(0, 50)).dpToPx()
        val params = FrameLayout.LayoutParams(size, size)
        cockroachView.layoutParams = params

        val maxX = gameAreaWidth - size
        val maxY = gameAreaHeight - size

        if (maxX > 0 && maxY > 0) {
            cockroachView.x = Random.nextInt(0, maxX).toFloat()
            cockroachView.y = Random.nextInt(0, maxY).toFloat()

            gameArea.addView(cockroachView)
            cockroaches.add(cockroachView)

            cockroachView.setOnClickListener {
                if (isGameRunning) {
                    if (isGold) {
                        // НОВОЕ: Очки пропорционально курсу золота (делим на 100 для баланса)
                        val points = (currentGoldPrice / 100).toInt().coerceAtLeast(10)
                        updateScore(points)
                        Toast.makeText(this, "+$points Gold!", Toast.LENGTH_SHORT).show()
                    } else {
                        updateScore(2)
                    }
                    gameArea.removeView(it)
                    cockroaches.remove(it)
                }
            }

            if (!isBonusActive) {
                moveCockroach(cockroachView, maxX, maxY)
            }
        }
    }

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
        if (score < 0) score = 0
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
        if (::gameTimer.isInitialized) gameTimer.cancel()
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
        if (!isBonusActive || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val gravityX = event.values[0]
        val gravityY = event.values[1]

        cockroaches.forEach { cockroach ->
            val bugSize = cockroach.width.toFloat()
            var newX = cockroach.x - (gravityX * TILT_SENSITIVITY)
            var newY = cockroach.y + (gravityY * TILT_SENSITIVITY)
            newX = newX.coerceIn(0f, gameAreaWidth - bugSize)
            newY = newY.coerceIn(0f, gameAreaHeight - bugSize)
            cockroach.x = newX
            cockroach.y = newY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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