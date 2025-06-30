package com.ext.videoplayerlibrary

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Rational
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.math.abs

class VideoPlayerLibrary @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Views
    private lateinit var videoView: VideoView
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var playPauseButton: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeTextView: TextView
    private lateinit var totalTimeTextView: TextView
    private lateinit var controlsContainer: ViewGroup
    private lateinit var tvLockRemove: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var ivScreenRotation: ImageView
    private lateinit var ivFullScreen: ImageView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var volumeBrightnessIndicator: LinearLayout
    private lateinit var ivVolumeBrightnessIcon: ImageView
    private lateinit var pbVolumeBrightness: ProgressBar
    private var autoPlay = false
    private var wasPlayingBeforeRotation = false
    private var defaultPlayState = false

    // Gesture Detection
    private lateinit var gestureDetector: GestureDetector

    // Properties
    private var videoTitle: String = ""
    private var videoSubtitle: String = ""
    private var videoUri: Uri? = null
    private var isPlaying = false
    private var currentPosition = 0
    private var totalDuration = 0
    private var isLocked = false
    private var isFullScreen = false
    private var currentPlaybackSpeed = 1.0f
    private var currentQuality = "Auto"
    private var currentSubtitle = "None"
    private var isInPipMode = false

    // Available options
    private val speedOptions = arrayOf("0.5X", "0.75X", "1X", "1.25X", "1.5X", "1.75X", "2X", "2.5X", "3X")
    private val speedValues = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    private var qualityOptions = arrayListOf<String>()
    private var subtitleOptions = arrayListOf<String>()

    // Customizable attributes
    private var primaryColor: Int = 0
    private var secondaryColor: Int = 0
    private var backgroundColor: Int = 0
    private var textColor: Int = 0
    private var titleTextSize: Float = 0f
    private var timeTextSize: Float = 0f
    private var showControls: Boolean = true
    private var autoHideControls: Boolean = true
    private var controlsTimeout: Long = 3000L
    private var seekStepSize: Int = 10000
    private var swipeSeekSensitivity: Float = 1.0f
    private var volumeStepSize: Float = 0.1f
    private var brightnessStepSize: Float = 0.1f

    // Handlers
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 1000)
        }
    }

    private val hideVolumeIndicatorRunnable = Runnable {
        volumeBrightnessIndicator.visibility = View.GONE
    }

    // Touch handling for lock
    private var lockTouchStartTime = 0L
    private val lockHoldDuration = 5000L // 5 seconds

    // Listeners
    private var onVideoPlayerListener: OnVideoPlayerListener? = null

    init {
        initializeView(attrs)
        setupGestureDetector()
    }

    private fun initializeView(attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.view_video_player_library, this, true)

        // Initialize views
        videoView = findViewById(R.id.video_view)
        titleTextView = findViewById(R.id.tv_video_title)
        subtitleTextView = findViewById(R.id.tv_video_subtitle)
        playPauseButton = findViewById(R.id.btn_play_pause)
        seekBar = findViewById(R.id.seek_bar)
        currentTimeTextView = findViewById(R.id.tv_current_time)
        totalTimeTextView = findViewById(R.id.tv_total_time)
        controlsContainer = findViewById(R.id.controls_container)
        tvLockRemove = findViewById(R.id.tvLockRemove)
        tvSpeed = findViewById(R.id.tv_speed)
        ivScreenRotation = findViewById(R.id.iv_screen_rotation)
        ivFullScreen = findViewById(R.id.iv_full_screen)
        loadingIndicator = findViewById(R.id.loading_indicator)
        volumeBrightnessIndicator = findViewById(R.id.volume_brightness_indicator)
        ivVolumeBrightnessIcon = findViewById(R.id.iv_volume_brightness_icon)
        pbVolumeBrightness = findViewById(R.id.pb_volume_brightness)

        // Load custom attributes
        loadAttributes(attrs)

        // Setup video view
        setupVideoView()

        // Setup controls
        setupControls()

        // Setup new controls
        setupAdvancedControls()

        // Keep screen on
        keepScreenOn = true
    }

    private fun loadAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.VideoPlayerLibrary)

            primaryColor = typedArray.getColor(
                R.styleable.VideoPlayerLibrary_vpl_primaryColor,
                ContextCompat.getColor(context, R.color.white)
            )

            secondaryColor = typedArray.getColor(
                R.styleable.VideoPlayerLibrary_vpl_secondaryColor,
                ContextCompat.getColor(context, R.color.black)
            )

            backgroundColor = typedArray.getColor(
                R.styleable.VideoPlayerLibrary_vpl_backgroundColor,
                ContextCompat.getColor(context, R.color.grey)
            )

            textColor = typedArray.getColor(
                R.styleable.VideoPlayerLibrary_vpl_textColor,
                ContextCompat.getColor(context, R.color.white)
            )

            titleTextSize = typedArray.getDimension(
                R.styleable.VideoPlayerLibrary_vpl_titleTextSize,
                resources.getDimension(R.dimen.vpl_title_text_size)
            )

            timeTextSize = typedArray.getDimension(
                R.styleable.VideoPlayerLibrary_vpl_timeTextSize,
                resources.getDimension(R.dimen.vpl_time_text_size)
            )

            showControls = typedArray.getBoolean(
                R.styleable.VideoPlayerLibrary_vpl_showControls,
                true
            )

            autoHideControls = typedArray.getBoolean(
                R.styleable.VideoPlayerLibrary_vpl_autoHideControls,
                true
            )

            controlsTimeout = typedArray.getInteger(
                R.styleable.VideoPlayerLibrary_vpl_controlsTimeout,
                3000
            ).toLong()

            seekStepSize = typedArray.getInteger(
                R.styleable.VideoPlayerLibrary_vpl_seekStepSize,
                10000
            )

            swipeSeekSensitivity = typedArray.getFloat(
                R.styleable.VideoPlayerLibrary_vpl_swipeSeekSensitivity,
                1.0f
            )

            volumeStepSize = typedArray.getFloat(
                R.styleable.VideoPlayerLibrary_vpl_volumeStepSize,
                0.1f
            )

            brightnessStepSize = typedArray.getFloat(
                R.styleable.VideoPlayerLibrary_vpl_brightnessStepSize,
                0.1f
            )

            videoTitle = typedArray.getString(R.styleable.VideoPlayerLibrary_vpl_videoTitle) ?: ""

            typedArray.recycle()
        }

        applyAttributes()
    }

    private fun applyAttributes() {
        // Apply colors and styles
        titleTextView.apply {
            setTextColor(textColor)
            textSize = titleTextSize / resources.displayMetrics.scaledDensity
            text = videoTitle
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        subtitleTextView.apply {
            setTextColor(textColor)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            text = videoSubtitle
        }
        currentTimeTextView.apply {
            setTextColor(textColor)
            textSize = timeTextSize / resources.displayMetrics.scaledDensity
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        totalTimeTextView.apply {
            setTextColor(textColor)
            textSize = timeTextSize / resources.displayMetrics.scaledDensity
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        tvSpeed.apply {
            setTextColor(textColor)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        playPauseButton.setColorFilter(primaryColor)

        seekBar.apply {
            progressTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            thumbTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        ivScreenRotation.setColorFilter(primaryColor)
        ivFullScreen.apply {
            setColorFilter(primaryColor)
            setOnClickListener {
                toggleFullScreen()
            }
        }

        // Make backgrounds transparent
        videoView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        controlsContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        this.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        // Show/hide controls
        controlsContainer.visibility = if (showControls) View.VISIBLE else View.GONE
    }

    private fun setupVideoView() {
        videoView.setOnPreparedListener { mediaPlayer ->
            totalDuration = mediaPlayer.duration
            totalTimeTextView.text = formatTime(totalDuration)
            seekBar.max = totalDuration

            // Set playback speed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(currentPlaybackSpeed)
            }

            loadingIndicator.visibility = View.GONE
            onVideoPlayerListener?.onVideoPrepared(totalDuration)

            // Extract quality options
            extractVideoQuality()

            // Restore position if coming from rotation
            if (currentPosition > 0) {
                videoView.seekTo(currentPosition)
                updateSeekBar()
            }

            // Only auto-play if explicitly set or restoring from rotation
            if (autoPlay || wasPlayingBeforeRotation) {
                play()
                wasPlayingBeforeRotation = false
            } else {
                // Default state is paused
                pause()
            }

            if (autoHideControls) {
                scheduleHideControls()
            }
        }

        videoView.setOnCompletionListener {
            onVideoPlayerListener?.onVideoCompleted()
            isPlaying = false
            updatePlayPauseButton()
        }

        videoView.setOnErrorListener { _, what, extra ->
            loadingIndicator.visibility = View.GONE
            onVideoPlayerListener?.onVideoError(what, extra)
            true
        }

        videoView.setOnInfoListener { _, what, extra ->
            when (what) {
                android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                    loadingIndicator.visibility = View.VISIBLE
                }
                android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                    loadingIndicator.visibility = View.GONE
                }
            }
            false
        }
    }
    private fun setupControls() {
        playPauseButton.setOnClickListener {
            if (!isLocked) {
                togglePlayPause()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isLocked) {
                    currentTimeTextView.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (!isLocked) {
                    handler.removeCallbacks(updateSeekBarRunnable)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (!isLocked) {
                    seekBar?.progress?.let { progress ->
                        videoView.seekTo(progress)
                        currentPosition = progress
                        onVideoPlayerListener?.onSeekTo(progress)
                    }
                    handler.post(updateSeekBarRunnable)
                }
            }
        })
    }

    private fun setupAdvancedControls() {


        // Speed Control
        tvSpeed.setOnClickListener {
            if (!isLocked) {
                showSpeedSpinner()
            }
        }



        // Screen Rotation
        ivScreenRotation.setOnClickListener {
            if (!isLocked) {
                toggleScreenOrientation()
            }
        }


        // Full Screen
        ivFullScreen.setOnClickListener {
            if (!isLocked) {
                toggleFullScreen()
            }
        }

        // Long press for lock removal

    }

    private fun showQualitySpinner() {
        if (qualityOptions.isEmpty()) {
            qualityOptions.add("Auto")
            qualityOptions.add("720p")
            qualityOptions.add("480p")
            qualityOptions.add("360p")
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, qualityOptions)
        val spinner = Spinner(context)
        spinner.adapter = adapter
        spinner.setSelection(qualityOptions.indexOf(currentQuality))

        val alertDialog = android.app.AlertDialog.Builder(context)
            .setTitle("Select Quality")
            .setView(spinner)
            .setPositiveButton("OK") { _, _ ->
                val selectedQuality = qualityOptions[spinner.selectedItemPosition]
                currentQuality = selectedQuality
                onVideoPlayerListener?.onQualityChanged(selectedQuality)
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun showSpeedSpinner() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, speedOptions)
        val spinner = Spinner(context)
        spinner.adapter = adapter

        // Find current speed index
        val currentSpeedIndex = speedValues.indexOf(currentPlaybackSpeed)
        spinner.setSelection(if (currentSpeedIndex >= 0) currentSpeedIndex else 2) // Default to 1X

        val alertDialog = android.app.AlertDialog.Builder(context)
            .setTitle("Select Speed")
            .setView(spinner)
            .setPositiveButton("OK") { _, _ ->
                val selectedIndex = spinner.selectedItemPosition
                currentPlaybackSpeed = speedValues[selectedIndex]
                tvSpeed.text = speedOptions[selectedIndex]

                // Apply speed change
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isPlaying) {
                    try {
                        val mediaPlayer = videoView.tag as? android.media.MediaPlayer
                        mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(currentPlaybackSpeed)!!
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                onVideoPlayerListener?.onSpeedChanged(currentPlaybackSpeed)
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun showSubtitleSpinner() {
        if (subtitleOptions.isEmpty()) {
            subtitleOptions.add("None")
            subtitleOptions.add("English")
            subtitleOptions.add("Spanish")
            subtitleOptions.add("French")
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, subtitleOptions)
        val spinner = Spinner(context)
        spinner.adapter = adapter
        spinner.setSelection(subtitleOptions.indexOf(currentSubtitle))

        val alertDialog = android.app.AlertDialog.Builder(context)
            .setTitle("Select Subtitle")
            .setView(spinner)
            .setPositiveButton("OK") { _, _ ->
                val selectedSubtitle = subtitleOptions[spinner.selectedItemPosition]
                currentSubtitle = selectedSubtitle
                onVideoPlayerListener?.onSubtitleChanged(selectedSubtitle)
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun toggleScreenOrientation() {
        // Toggle screen orientation
        val activity = context as? Activity
        activity?.requestedOrientation = if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun toggleFullScreen() {
        val activity = context as? Activity ?: return

        isFullScreen = !isFullScreen

        if (isFullScreen) {
            // Enter full screen mode
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            
            // Adjust layout for full screen
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Adjust controls visibility and size
            controlsContainer.apply {
                visibility = View.VISIBLE
                alpha = 0.8f
            }

            // Adjust video view size
            videoView.apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                requestLayout()
            }

            // Keep screen on in full screen mode
            keepScreenOn = true
        } else {
            // Exit full screen mode
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            // Adjust layout for normal mode
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Restore normal controls
            controlsContainer.apply {
                alpha = 1.0f
            }

            // Adjust video view size
            videoView.apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                requestLayout()
            }

            // Restore screen on behavior
            keepScreenOn = false
        }

        onVideoPlayerListener?.onFullScreenChanged(isFullScreen)
    }

    private fun extractVideoQuality() {
        try {
            videoUri?.let { uri ->
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()

                qualityOptions.clear()
                qualityOptions.add("Auto")

                if (width != null && height != null) {
                    when {
                        height >= 1080 -> {
                            qualityOptions.add("1080p")
                            qualityOptions.add("720p")
                            qualityOptions.add("480p")
                            qualityOptions.add("360p")
                        }
                        height >= 720 -> {
                            qualityOptions.add("720p")
                            qualityOptions.add("480p")
                            qualityOptions.add("360p")
                        }
                        height >= 480 -> {
                            qualityOptions.add("480p")
                            qualityOptions.add("360p")
                        }
                        else -> {
                            qualityOptions.add("360p")
                        }
                    }
                }

                retriever.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            qualityOptions.clear()
            qualityOptions.add("Auto")
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isLocked) {
                    toggleControlsVisibility()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isLocked) {
                    val screenWidth = width
                    val tapX = e.x

                    when {
                        tapX < screenWidth * 0.3f -> {
                            seekBackward()
                        }
                        tapX > screenWidth * 0.7f -> {
                            seekForward()
                        }
                        else -> {
                            togglePlayPause()
                        }
                    }
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isLocked) {
                    e1?.let { startEvent ->
                        val screenWidth = width
                        val startX = startEvent.x
                        val deltaX = e2.x - startEvent.x
                        val deltaY = startEvent.y - e2.y

                        when {
                            abs(deltaX) > abs(deltaY) && abs(deltaX) > 50 -> {
                                val seekAmount = (deltaX * swipeSeekSensitivity).toInt()
                                seekByAmount(seekAmount)
                            }
                            startX < screenWidth * 0.5f && abs(deltaY) > 50 -> {
                                adjustBrightness(deltaY)
                            }
                            startX >= screenWidth * 0.5f && abs(deltaY) > 50 -> {
                                adjustVolume(deltaY)
                            }
                        }
                    }
                }
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        videoView.start()
        isPlaying = true
        updatePlayPauseButton()
        handler.post(updateSeekBarRunnable)
        onVideoPlayerListener?.onVideoStarted()

        if (autoHideControls) {
            scheduleHideControls()
        }
    }

    private fun pause() {
        videoView.pause()
        isPlaying = false
        updatePlayPauseButton()
        handler.removeCallbacks(updateSeekBarRunnable)
        onVideoPlayerListener?.onVideoPaused()

        handler.removeCallbacks(hideControlsRunnable)
    }

    private fun updatePlayPauseButton() {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        playPauseButton.setImageResource(iconRes)
    }

    private fun seekForward() {
        val newPosition = (currentPosition + seekStepSize).coerceAtMost(totalDuration)
        videoView.seekTo(newPosition)
        currentPosition = newPosition
        onVideoPlayerListener?.onSeekTo(newPosition)
    }

    private fun seekBackward() {
        val newPosition = (currentPosition - seekStepSize).coerceAtLeast(0)
        videoView.seekTo(newPosition)
        currentPosition = newPosition
        onVideoPlayerListener?.onSeekTo(newPosition)
    }

    private fun seekByAmount(amount: Int) {
        val newPosition = (currentPosition + amount).coerceIn(0, totalDuration)
        videoView.seekTo(newPosition)
        currentPosition = newPosition
        seekBar.progress = newPosition
        currentTimeTextView.text = formatTime(newPosition)
        onVideoPlayerListener?.onSeekTo(newPosition)
    }

    private fun adjustBrightness(deltaY: Float) {
        try {
            val activity = context as? Activity ?: return
            val window = activity.window
            val attributes = window.attributes

            val brightnessChange = deltaY * brightnessStepSize / 100f
            attributes.screenBrightness = (attributes.screenBrightness + brightnessChange).coerceIn(0.01f, 1.0f)

            window.attributes = attributes

            // Show brightness indicator
            showVolumeOrBrightnessIndicator(false, (attributes.screenBrightness * 100).toInt())

            onVideoPlayerListener?.onBrightnessChanged(attributes.screenBrightness)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun adjustVolume(deltaY: Float) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)

            val volumeChange = (deltaY * volumeStepSize * maxVolume / 100f).toInt()
            val newVolume = (currentVolume + volumeChange).coerceIn(0, maxVolume)

            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)

            // Show volume indicator
            showVolumeOrBrightnessIndicator(true, (newVolume * 100 / maxVolume))

            onVideoPlayerListener?.onVolumeChanged(newVolume, maxVolume)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun showVolumeOrBrightnessIndicator(isVolume: Boolean, value: Int) {
        volumeBrightnessIndicator.visibility = View.VISIBLE

        // Set appropriate icon
        val iconRes = if (isVolume) R.drawable.ic_volume else R.drawable.ic_brightness
        ivVolumeBrightnessIcon.setImageResource(iconRes)

        // Set progress
        pbVolumeBrightness.progress = value

        // Auto hide after 2 seconds
        handler.removeCallbacks(hideVolumeIndicatorRunnable)
        handler.postDelayed(hideVolumeIndicatorRunnable, 2000)
    }

    private fun toggleControlsVisibility() {
        if (controlsContainer.visibility == View.VISIBLE) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        if (!isLocked) {
            controlsContainer.visibility = View.VISIBLE
            if (autoHideControls) {
                scheduleHideControls()
            }
        }
    }

    private fun hideControls() {
        if (!isLocked) {
            controlsContainer.visibility = View.GONE
            handler.removeCallbacks(hideControlsRunnable)
        }
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, controlsTimeout)
    }

    private fun updateSeekBar() {
        if (isPlaying && !isLocked) {
            currentPosition = videoView.currentPosition
            seekBar.progress = currentPosition
            currentTimeTextView.text = formatTime(currentPosition)
            onVideoPlayerListener?.onProgressUpdated(currentPosition, totalDuration)
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // Public API methods
    fun setVideoUri(uri: Uri) {
        this.videoUri = uri
        videoView.setVideoURI(uri)
        loadingIndicator.visibility = View.VISIBLE
        // Reset auto-play flag unless explicitly set
        if (!autoPlay) {
            wasPlayingBeforeRotation = false
        }
    }
    fun setVideoTitle(title: String) {
        this.videoTitle = title
        titleTextView.text = title
    }

    fun setVideoSubtitle(subtitle: String) {
        this.videoSubtitle = subtitle
        titleTextView.text = subtitle
    }

    fun setOnVideoPlayerListener(listener: OnVideoPlayerListener) {
        this.onVideoPlayerListener = listener
    }

    fun getCurrentPosition(): Int = currentPosition

    fun getDuration(): Int = totalDuration

    fun isVideoPlaying(): Boolean = isPlaying

    fun isPlayerLocked(): Boolean = isLocked

    fun isInFullScreen(): Boolean = isFullScreen

    fun seekToPosition(position: Int) {
        videoView.seekTo(position)
        currentPosition = position
        onVideoPlayerListener?.onSeekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed
        val speedText = "${speed}X"
        tvSpeed.text = speedText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isPlaying) {
            try {
                val mediaPlayer = videoView.tag as? android.media.MediaPlayer
                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(currentPlaybackSpeed)!!
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setQualityOptions(options: ArrayList<String>) {
        qualityOptions.clear()
        qualityOptions.addAll(options)
    }

    fun setSubtitleOptions(options: ArrayList<String>) {
        subtitleOptions.clear()
        subtitleOptions.addAll(options)
    }

    fun getCurrentQuality(): String = currentQuality

    fun getCurrentSubtitle(): String = currentSubtitle

    fun getCurrentSpeed(): Float = currentPlaybackSpeed

    fun enterFullScreenMode() {
        if (!isFullScreen) {
            toggleFullScreen()
        }
    }

    fun exitFullScreenMode() {
        if (isFullScreen) {
            toggleFullScreen()
        }
    }




    // Handle configuration changes
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        // Store current state
        val wasPlaying = isPlaying
        if (wasPlaying) {
            currentPosition = videoView.currentPosition
        }

        // Adjust layout for orientation
        when (newConfig?.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                titleTextView.textSize = (timeTextSize / resources.displayMetrics.scaledDensity) * 0.8f
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                titleTextView.textSize = titleTextSize / resources.displayMetrics.scaledDensity
            }
        }

        // Restore state after a short delay
        handler.postDelayed({
            if (currentPosition > 0) {
                videoView.seekTo(currentPosition)
                if (wasPlaying) {
                    wasPlayingBeforeRotation = true
                }
            }
        }, 100)
    }
    // Handle Picture-in-Picture mode changes
    @RequiresApi(Build.VERSION_CODES.O)
    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        isInPipMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            // Hide controls in PiP mode
            controlsContainer.visibility = View.GONE
        } else {
            // Show controls when exiting PiP mode
            if (!isLocked) {
                controlsContainer.visibility = View.VISIBLE
                if (autoHideControls) {
                    scheduleHideControls()
                }
            }
        }

        onVideoPlayerListener?.onPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    // Cleanup resources
    fun release() {
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(hideVolumeIndicatorRunnable)

        videoView.stopPlayback()
        onVideoPlayerListener = null
    }

    // Pause video when view is not visible
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            pauseVideo()
        } else if (visibility == View.VISIBLE) {
            if (wasPlayingBeforeRotation) {
                playVideo()
            }
        }
    }

    // Interface for video player events
    interface OnVideoPlayerListener {
        fun onVideoPrepared(duration: Int) {}
        fun onVideoStarted() {}
        fun onVideoPaused() {}
        fun onVideoCompleted() {}
        fun onVideoError(what: Int, extra: Int) {}
        fun onSeekTo(position: Int) {}
        fun onProgressUpdated(currentPosition: Int, duration: Int) {}
        fun onSpeedChanged(speed: Float) {}
        fun onQualityChanged(quality: String) {}
        fun onSubtitleChanged(subtitle: String) {}
        fun onOrientationChanged(orientation: Int) {}
        fun onFullScreenChanged(isFullScreen: Boolean) {}
        fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {}
        fun onPlayerLocked(isLocked: Boolean) {}
        fun onVolumeChanged(volume: Int, maxVolume: Int) {}
        fun onBrightnessChanged(brightness: Float) {}
    }

    // Save and restore state
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = Bundle()
        savedState.putParcelable("super_state", superState)
        savedState.putInt("current_position", currentPosition)
        savedState.putBoolean("is_playing", isPlaying)
        savedState.putBoolean("is_locked", isLocked)
        savedState.putBoolean("is_full_screen", isFullScreen)
        savedState.putFloat("playback_speed", currentPlaybackSpeed)
        savedState.putString("current_quality", currentQuality)
        savedState.putString("current_subtitle", currentSubtitle)
        savedState.putString("video_title", videoTitle)
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super_state"))
            currentPosition = state.getInt("current_position", 0)
            isPlaying = state.getBoolean("is_playing", false)
            isLocked = state.getBoolean("is_locked", false)
            isFullScreen = state.getBoolean("is_full_screen", false)
            currentPlaybackSpeed = state.getFloat("playback_speed", 1.0f)
            currentQuality = state.getString("current_quality", "Auto") ?: "Auto"
            currentSubtitle = state.getString("current_subtitle", "None") ?: "None"
            videoTitle = state.getString("video_title", "") ?: ""

            // Restore UI state
            updatePlayPauseButton()
            tvSpeed.text = "${currentPlaybackSpeed}X"
            titleTextView.text = videoTitle


            if (currentPosition > 0) {
                handler.postDelayed({
                    videoView.seekTo(currentPosition)
                }, 100)
            }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    // Handle back press in full screen mode
    fun onBackPressed(): Boolean {
        return if (isFullScreen) {
            exitFullScreenMode()
            true
        } else {
            false
        }
    }

    // Utility methods for external access
    fun showControlsManually() {
        showControls()
    }

    fun hideControlsManually() {
        hideControls()
    }

    fun setControlsTimeout(timeout: Long) {
        this.controlsTimeout = timeout
    }

    fun setSeekStepSize(stepSize: Int) {
        this.seekStepSize = stepSize
    }

    fun setSwipeSeekSensitivity(sensitivity: Float) {
        this.swipeSeekSensitivity = sensitivity
    }

    fun setVolumeStepSize(stepSize: Float) {
        this.volumeStepSize = stepSize
    }

    fun setBrightnessStepSize(stepSize: Float) {
        this.brightnessStepSize = stepSize
    }

    // Method to handle activity lifecycle
    fun onResume() {
        if (isPlaying) {
            handler.post(updateSeekBarRunnable)
        }
    }

    fun onPause() {
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(hideControlsRunnable)
    }

    fun onDestroy() {
        release()
    }

    fun setAutoPlay(autoPlay: Boolean) {
        this.autoPlay = autoPlay
    }

    fun getAutoPlay(): Boolean = autoPlay

    fun playVideo() {
        if (!isPlaying) {
            play()
        }
    }

    fun pauseVideo() {
        if (isPlaying) {
            pause()
        }
    }

    fun togglePlayPauseManually() {
        togglePlayPause()
    }

    fun isVideoReady(): Boolean {
        return totalDuration > 0
    }

    fun setVideoUriWithAutoPlay(uri: Uri, autoPlay: Boolean = false) {
        this.autoPlay = autoPlay
        setVideoUri(uri)
    }
}