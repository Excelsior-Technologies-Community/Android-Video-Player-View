package com.ext.videoplayerlibrarytestingapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ext.videoplayerlibrary.VideoPlayerLibrary

class MainActivity : AppCompatActivity() {

    private lateinit var videoPlayer: VideoPlayerLibrary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the video player
        videoPlayer = findViewById(R.id.videoPlayer)

        // Set up the video player
        setupVideoPlayer()

        // Load and play video
        loadVideo()
    }

    private fun setupVideoPlayer() {
        // Configure player settings using available methods
        videoPlayer.setControlsTimeout(5000L) // 5 seconds
        videoPlayer.setSeekStepSize(15000) // 15 seconds
        videoPlayer.setSwipeSeekSensitivity(1.2f)
        videoPlayer.setVolumeStepSize(0.05f)
        videoPlayer.setBrightnessStepSize(0.05f)

        // Set video player listener
        videoPlayer.setOnVideoPlayerListener(object : VideoPlayerLibrary.OnVideoPlayerListener {
            override fun onVideoPrepared(duration: Int) {
                println("Video prepared with duration: ${duration}ms")
                // Convert duration to readable format
                val minutes = duration / 1000 / 60
                val seconds = (duration / 1000) % 60
                println("Duration: ${minutes}:${String.format("%02d", seconds)}")
            }

            override fun onVideoStarted() {
                println("Video started playing")
            }

            override fun onVideoPaused() {
                println("Video paused")
            }

            override fun onVideoCompleted() {
                println("Video playback completed")
                // Optionally restart or show completion message
            }

            override fun onVideoError(what: Int, extra: Int) {
                println("Video error: what=$what, extra=$extra")
                // Handle error - maybe show error message to user
            }

            override fun onSeekTo(position: Int) {
                println("Seeked to position: ${position}ms")
            }

            override fun onProgressUpdated(currentPosition: Int, duration: Int) {
                // This is called every second during playback
                // You can use this for custom progress tracking
            }

            override fun onSpeedChanged(speed: Float) {
                println("Playback speed changed to: ${speed}x")
            }

            override fun onQualityChanged(quality: String) {
                println("Video quality changed to: $quality")
            }

            override fun onSubtitleChanged(subtitle: String) {
                println("Subtitle changed to: $subtitle")
            }

            override fun onOrientationChanged(orientation: Int) {
                println("Screen orientation changed to: $orientation")
            }

            override fun onFullScreenChanged(isFullScreen: Boolean) {
                println("Full screen mode: $isFullScreen")
                if (isFullScreen) {
                    // Hide status bar or action bar if needed
                    supportActionBar?.hide()
                } else {
                    // Show status bar or action bar
                    supportActionBar?.show()
                }
            }

            override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
                println("Picture-in-Picture mode: $isInPictureInPictureMode")
            }

            override fun onPlayerLocked(isLocked: Boolean) {
                println("Player locked: $isLocked")
            }

            override fun onVolumeChanged(volume: Int, maxVolume: Int) {
                println("Volume changed to: $volume/$maxVolume")
                val volumePercentage = (volume * 100) / maxVolume
                println("Volume percentage: $volumePercentage%")
            }

            override fun onBrightnessChanged(brightness: Float) {
                println("Brightness changed to: $brightness")
                val brightnessPercentage = (brightness * 100).toInt()
                println("Brightness percentage: $brightnessPercentage%")
            }
        })
    }

    private fun loadVideo() {
        // Option 1: Load from raw resource
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.sample_video}")
        videoPlayer.setVideoUri(videoUri)

        // Option 2: Load from assets folder
        // val videoUri = Uri.parse("android.asset://sample_video.mp4")
        // videoPlayer.setVideoUri(videoUri)

        // Option 3: Load from external URL
        // val videoUri = Uri.parse("https://example.com/sample_video.mp4")
        // videoPlayer.setVideoUri(videoUri)

        // Option 4: Load from file path
        // val videoUri = Uri.parse("file:///android_asset/sample_video.mp4")
        // videoPlayer.setVideoUri(videoUri)

        // Set video title
        videoPlayer.setVideoTitle("Sample Video Title")

        // Set quality options (optional)
        val qualityOptions = arrayListOf("Auto", "1080p", "720p", "480p", "360p")
        videoPlayer.setQualityOptions(qualityOptions)

        // Set subtitle options (optional)
        val subtitleOptions = arrayListOf("None", "English", "Spanish", "French")
        videoPlayer.setSubtitleOptions(subtitleOptions)

        // Set initial playback speed (optional)
        videoPlayer.setPlaybackSpeed(1.0f)
    }

    override fun onPause() {
        super.onPause()
        // The library handles pause/resume automatically
        videoPlayer.onPause()
    }

    override fun onResume() {
        super.onResume()
        // The library handles pause/resume automatically
        videoPlayer.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        videoPlayer.onDestroy()
    }

    override fun onBackPressed() {
        // Handle back press for full screen mode
        if (!videoPlayer.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        // Handle PiP mode changes
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            videoPlayer.onPictureInPictureModeChanged(isInPictureInPictureMode)
        }
    }

    // Helper methods for external control (optional)
    private fun playVideo() {
        if (!videoPlayer.isVideoPlaying()) {
            // Trigger play by simulating tap on play button
            // The library handles play/pause internally
        }
    }

    private fun pauseVideo() {
        if (videoPlayer.isVideoPlaying()) {
            // Trigger pause by simulating tap on pause button
            // The library handles play/pause internally
        }
    }

    private fun seekToPosition(positionMs: Int) {
        videoPlayer.seekToPosition(positionMs)
    }

    private fun getCurrentPosition(): Int {
        return videoPlayer.getCurrentPosition()
    }

    private fun getDuration(): Int {
        return videoPlayer.getDuration()
    }

    private fun isPlaying(): Boolean {
        return videoPlayer.isVideoPlaying()
    }

    private fun enterFullScreen() {
        videoPlayer.enterFullScreenMode()
    }

    private fun exitFullScreen() {
        videoPlayer.exitFullScreenMode()
    }

    private fun lockPlayer() {
        videoPlayer.lockPlayer()
    }

    private fun unlockPlayer() {
        videoPlayer.unlockPlayer()
    }

    private fun showControls() {
        videoPlayer.showControlsManually()
    }

    private fun hideControls() {
        videoPlayer.hideControlsManually()
    }
}