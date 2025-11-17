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
import com.example.bugs.viewmodel.GameViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel // Импорт Koin
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
        private const val GOLD_BUG_INTERVAL = 20000L
    }

    // --- INJECTION: Получаем ViewModel через Koin ---
    private val viewModel: GameViewModel by viewModel()

    // UI
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var playerNameTextView: TextView
    private lateinit var gameArea: FrameLayout

    // Params
    private var currentPlayer: Player? = null
    private var gameSpeed = 5
    private var maxCockroaches = 10
    private var bonusInterval = 15000L

    // Game Loop Helpers (UI related only)
    private val cockroaches = mutableListOf<ImageView>()
    private val gameHandler = Handler(Looper.getMainLooper())

    // Sensors & Audio
    private var bonusView: ImageView? = null
    private var isBonusActive = false
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var soundPool: SoundPool? = null
    private var bugScreamSoundId: Int = 0
    private var gameAreaWidth = 0
    private var gameAreaHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Init UI
        scoreTextView = findViewById(R.id.textViewScore)
        timerTextView = findViewById(R.id.textViewTimer)
        playerNameTextView = findViewById(R.id.textViewPlayerName)
        gameArea = findViewById(R.id.gameArea)

        // Get Intent Data
        currentPlayer = getPlayerFromIntent()
        gameSpeed = intent.getIntExtra(EXTRA_GAME_SPEED, 5)
        maxCockroaches = intent.getIntExtra(EXTRA_MAX_COCKROACHES, 10)
        val roundDuration = intent.getIntExtra(EXTRA_ROUND_DURATION, 120).toLong()
        bonusInterval = intent.getIntExtra(EXTRA_BONUS_INTERVAL, 15).toLong() * 1000

        if (currentPlayer == null) {
            finish()
            return
        }
        playerNameTextView.text = "Игрок: ${currentPlayer!!.name}"

        setupSensors()
        setupSoundPool()
        setupObservers() // Подписываемся на обновления ViewModel

        // Обработка клика по фону (промах)
        gameArea.setOnClickListener {
            if (viewModel.isGameRunning) {
                viewModel.addScore(-1)
            }
        }

        // Старт игры после отрисовки UI
        gameArea.post {
            gameAreaWidth = gameArea.width
            gameAreaHeight = gameArea.height

            // Если ViewModel говорит, что игра еще не запущена (первый запуск)
            if (!viewModel.isGameRunning) {
                viewModel.startGame(roundDuration)
            }
            // Если экран повернулся, игра уже Running = true, мы просто возобновляем анимации
            resumeGameLoops()
        }
    }

    private fun setupObservers() {
        // Слушаем изменение счета
        viewModel.score.observe(this) { newScore ->
            scoreTextView.text = "Счет: $newScore"
        }

        // Слушаем таймер
        viewModel.timeLeft.observe(this) { seconds ->
            timerTextView.text = "Время: $seconds"
        }

        // Слушаем конец игры
        viewModel.isGameOver.observe(this) { isOver ->
            if (isOver) showGameOverDialog()
        }
    }

    private fun resumeGameLoops() {
        // Запускаем циклы создания тараканов и бонусов
        gameHandler.removeCallbacksAndMessages(null) // Чистим старые, чтобы не дублировать
        gameHandler.post(spawner)
        gameHandler.postDelayed(bonusSpawner, bonusInterval)
        gameHandler.postDelayed(goldBugSpawner, GOLD_BUG_INTERVAL)
    }

    // --- Spawners (View Logic) ---

    private val spawner = object : Runnable {
        override fun run() {
            if (viewModel.isGameRunning && cockroaches.size < maxCockroaches) {
                spawnCockroach(isGold = false)
            }
            val spawnInterval = 1000L / gameSpeed
            gameHandler.postDelayed(this, spawnInterval)
        }
    }

    private val bonusSpawner = object : Runnable {
        override fun run() {
            if (viewModel.isGameRunning && bonusView == null && !isBonusActive) {
                spawnBonus()
            }
            gameHandler.postDelayed(this, bonusInterval)
        }
    }

    private val goldBugSpawner = object : Runnable {
        override fun run() {
            if (viewModel.isGameRunning) {
                spawnCockroach(isGold = true)
            }
            gameHandler.postDelayed(this, GOLD_BUG_INTERVAL)
        }
    }

    private fun spawnCockroach(isGold: Boolean) {
        val cockroachView = ImageView(this)
        cockroachView.setImageResource(R.drawable.cockroach)

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
                if (viewModel.isGameRunning) {
                    if (isGold) {
                        val points = (viewModel.currentGoldPrice / 100).toInt().coerceAtLeast(10)
                        viewModel.addScore(points)
                        Toast.makeText(this, "+$points Gold!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addScore(2)
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
                    if (viewModel.isGameRunning) {
                        activateBonusEffect()
                        gameArea.removeView(this)
                        bonusView = null
                    }
                }
            }
        }
        gameArea.addView(bonusView)
    }

    // --- Movement Logic ---

    private fun moveCockroach(cockroachView: ImageView, maxX: Int, maxY: Int) {
        if (!viewModel.isGameRunning || !cockroaches.contains(cockroachView) || isBonusActive) return

        val newX = Random.nextInt(0, maxX).toFloat()
        val newY = Random.nextInt(0, maxY).toFloat()
        val moveDuration = 2000L - (gameSpeed * 150)

        cockroachView.animate()
            .x(newX)
            .y(newY)
            .setDuration(moveDuration.coerceAtLeast(500L))
            .withEndAction {
                if (!isBonusActive) moveCockroach(cockroachView, maxX, maxY)
            }
            .start()
    }

    // --- Bonus & Sensors ---

    private fun activateBonusEffect() {
        if (accelerometer == null) return
        isBonusActive = true
        soundPool?.play(bugScreamSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        cockroaches.forEach { it.animate().cancel() }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Отключаем бонус через 1 сек
        Handler(Looper.getMainLooper()).postDelayed({ stopBonusEffect() }, BONUS_LIFETIME_MS)
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

    // --- Game Over & Cleanup ---

    private fun showGameOverDialog() {
        gameHandler.removeCallbacksAndMessages(null)
        if (isBonusActive) stopBonusEffect()

        // Сохраняем рекорд
        val finalScore = viewModel.score.value ?: 0
        currentPlayer?.let { PlayerManager.updatePlayerHighScore(it.name, finalScore) }

        cockroaches.forEach { gameArea.removeView(it) }
        cockroaches.clear()

        AlertDialog.Builder(this)
            .setTitle("Раунд окончен!")
            .setMessage("Ваш итоговый счет: $finalScore")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // При уничтожении Activity (даже при повороте) останавливаем хендлеры UI,
        // но ViewModel продолжает жить, если это поворот
        gameHandler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        soundPool?.release()
        soundPool = null
    }

    // --- Utils ---

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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

    private fun getPlayerFromIntent(): Player? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PLAYER, Player::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PLAYER)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}