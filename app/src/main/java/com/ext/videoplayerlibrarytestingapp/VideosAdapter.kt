package com.ext.videoplayerlibrarytestingapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ext.videoplayerlibrary.VideoPlayerLibrary

data class VideoItem(
    val videoUri: Uri,
    val title: String,
    val qualityOptions: ArrayList<String> = arrayListOf("Auto", "1080p", "720p", "480p", "360p"),
    val subtitleOptions: ArrayList<String> = arrayListOf("None", "English", "Spanish", "French")
)

class VideosAdapter : RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {

    private val videoList = mutableListOf<VideoItem>()
    private var currentPlayingPosition = -1
    private var currentPlayingViewHolder: VideoViewHolder? = null
    private val viewHolders = mutableMapOf<Int, VideoViewHolder>()

    // Add callback for external listeners
    interface OnVideoStateChangeListener {
        fun onVideoStarted(position: Int)
        fun onVideoPaused(position: Int)
        fun onVideoCompleted(position: Int)
    }

    private var videoStateChangeListener: OnVideoStateChangeListener? = null

    fun setOnVideoStateChangeListener(listener: OnVideoStateChangeListener) {
        this.videoStateChangeListener = listener
    }

    fun setVideoList(videos: List<VideoItem>) {
        // Pause current video before changing the list
        pauseAllVideos()

        // Clear view holders mapping since positions will change
        viewHolders.clear()

        videoList.clear()
        videoList.addAll(videos)
        notifyDataSetChanged()
    }

    fun addVideo(video: VideoItem) {
        videoList.add(video)
        notifyItemInserted(videoList.size - 1)
    }

    fun getCurrentPlayingPosition(): Int = currentPlayingPosition

    fun isVideoPlaying(): Boolean = currentPlayingPosition != -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_videos, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoList[position]
        viewHolders[position] = holder
        holder.bind(videoItem, position)
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            viewHolders.remove(position)
            // If this was the currently playing video, reset the current playing state
            if (position == currentPlayingPosition) {
                resetCurrentPlayingState()
            }
            holder.releasePlayer()
        }
    }

    override fun getItemCount(): Int = videoList.size

    // Enhanced pause method with better state management
    fun pauseAllVideos() {
        currentPlayingViewHolder?.let { holder ->
            if (holder.isPlaying()) {
                holder.pauseVideo()
            }
        }
        resetCurrentPlayingState()
    }

    // Private method to reset playing state
    private fun resetCurrentPlayingState() {
        currentPlayingPosition = -1
        currentPlayingViewHolder = null
    }

    // Release all players
    fun releaseAllPlayers() {
        viewHolders.values.forEach { holder ->
            holder.releasePlayer()
        }
        viewHolders.clear()
        resetCurrentPlayingState()
    }

    // Handle back press for full screen
    fun handleBackPress(): Boolean {
        return currentPlayingViewHolder?.handleBackPress() ?: false
    }

    // Activity lifecycle methods
    fun onActivityResume() {
        currentPlayingViewHolder?.onResume()
    }

    fun onActivityPause() {
        pauseAllVideos()
        viewHolders.values.forEach { it.onPause() }
    }

    // Enhanced method to handle video click and play
    private fun playVideoAtPosition(position: Int, holder: VideoViewHolder) {
        // Pause currently playing video if different from clicked one
        if (currentPlayingPosition != position) {
            pauseAllVideos()
        }

        // Update current playing state
        currentPlayingPosition = position
        currentPlayingViewHolder = holder

        // Start playing the clicked video
        holder.playVideo()
    }

    // Public method to play video at specific position (for external control)
    fun playVideoAt(position: Int): Boolean {
        val holder = viewHolders[position]
        return if (holder != null && position < videoList.size) {
            playVideoAtPosition(position, holder)
            true
        } else {
            false
        }
    }

    // Public method to pause video at specific position
    fun pauseVideoAt(position: Int): Boolean {
        val holder = viewHolders[position]
        return if (holder != null && holder.isPlaying()) {
            holder.pauseVideo()
            true
        } else {
            false
        }
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoPlayer: VideoPlayerLibrary = itemView.findViewById(R.id.videoPlayer)
        private var isPlayerInitialized = false
        private var isVideoLoaded = false
        private var currentVideoItem: VideoItem? = null

        init {
            // Set click listener on the video player to handle play/pause
            videoPlayer.setOnClickListener {
                handleVideoClick()
            }
        }

        fun bind(videoItem: VideoItem, position: Int) {
            try {
                currentVideoItem = videoItem
                setupVideoPlayer(videoItem, position)
                loadVideo(videoItem)
            } catch (e: Exception) {
                println("Error binding video at position $position: ${e.message}")
                e.printStackTrace()
            }
        }

        private fun setupVideoPlayer(videoItem: VideoItem, position: Int) {
            if (isPlayerInitialized) return

            try {
                // Configure player settings
                videoPlayer.setControlsTimeout(5000L)
                videoPlayer.setSeekStepSize(15000)
                videoPlayer.setSwipeSeekSensitivity(1.2f)
                videoPlayer.setVolumeStepSize(0.05f)
                videoPlayer.setBrightnessStepSize(0.05f)

                // Set video player listener with enhanced state management
                videoPlayer.setOnVideoPlayerListener(object : VideoPlayerLibrary.OnVideoPlayerListener {
                    override fun onVideoPrepared(duration: Int) {
                        println("Video prepared at position $position - Duration: ${duration}ms")
                        isVideoLoaded = true
                    }

                    override fun onVideoStarted() {
                        println("Video started at position $position")
                        // Ensure only this video is playing
                        if (currentPlayingPosition != position) {
                            // Pause other videos first
                            val previousHolder = currentPlayingViewHolder
                            if (previousHolder != null && previousHolder != this@VideoViewHolder) {
                                previousHolder.pauseVideo()
                            }

                            currentPlayingPosition = position
                            currentPlayingViewHolder = this@VideoViewHolder
                        }
                        videoStateChangeListener?.onVideoStarted(position)
                    }

                    override fun onVideoPaused() {
                        println("Video paused at position $position")
                        if (currentPlayingPosition == position) {
                            resetCurrentPlayingState()
                        }
                        videoStateChangeListener?.onVideoPaused(position)
                    }

                    override fun onVideoCompleted() {
                        println("Video completed at position $position")
                        if (currentPlayingPosition == position) {
                            resetCurrentPlayingState()
                        }
                        videoStateChangeListener?.onVideoCompleted(position)
                    }

                    override fun onVideoError(what: Int, extra: Int) {
                        println("Video error at position $position: what=$what, extra=$extra")
                        if (currentPlayingPosition == position) {
                            resetCurrentPlayingState()
                        }
                    }

                    override fun onSeekTo(position: Int) {
                        println("Seeked to position: ${position}ms")
                    }

                    override fun onProgressUpdated(currentPosition: Int, duration: Int) {
                        // Progress tracking can be implemented here if needed
                    }

                    override fun onSpeedChanged(speed: Float) {
                        println("Speed changed to: ${speed}x at position $position")
                    }

                    override fun onQualityChanged(quality: String) {
                        println("Quality changed to: $quality at position $position")
                    }

                    override fun onSubtitleChanged(subtitle: String) {
                        println("Subtitle changed to: $subtitle at position $position")
                    }

                    override fun onOrientationChanged(orientation: Int) {
                        println("Orientation changed at position $position")
                    }

                    override fun onFullScreenChanged(isFullScreen: Boolean) {
                        println("Full screen mode: $isFullScreen at position $position")
                    }

                    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
                        println("PiP mode: $isInPictureInPictureMode at position $position")
                    }

                    override fun onPlayerLocked(isLocked: Boolean) {
                        println("Player locked: $isLocked at position $position")
                    }

                    override fun onVolumeChanged(volume: Int, maxVolume: Int) {
                        println("Volume changed at position $position: $volume/$maxVolume")
                    }

                    override fun onBrightnessChanged(brightness: Float) {
                        println("Brightness changed at position $position: $brightness")
                    }
                })

                isPlayerInitialized = true
            } catch (e: Exception) {
                println("Error setting up video player at position $position: ${e.message}")
                e.printStackTrace()
            }
        }

        private fun loadVideo(videoItem: VideoItem) {
            if (!isPlayerInitialized) return

            try {
                // Set video properties
                videoPlayer.setVideoUri(videoItem.videoUri)
                videoPlayer.setVideoTitle(videoItem.title)
                videoPlayer.setQualityOptions(videoItem.qualityOptions)
                videoPlayer.setSubtitleOptions(videoItem.subtitleOptions)
                videoPlayer.setPlaybackSpeed(1.0f)

                // Ensure auto-play is disabled
                videoPlayer.setAutoPlay(false)
            } catch (e: Exception) {
                println("Error loading video: ${e.message}")
                e.printStackTrace()
            }
        }

        private fun handleVideoClick() {
            if (!isVideoLoaded) {
                println("Video not yet loaded at position $adapterPosition")
                return
            }

            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION) return

            if (isPlaying()) {
                // If this video is playing, pause it
                pauseVideo()
            } else {
                // If this video is not playing, play it (this will pause others)
                playVideoAtPosition(position, this)
            }
        }

        // Video control methods
        fun playVideo() {
            if (isVideoLoaded && !isPlaying()) {
                try {
                    videoPlayer.playVideo()
                } catch (e: Exception) {
                    println("Error playing video: ${e.message}")
                }
            }
        }

        fun pauseVideo() {
            if (isPlaying()) {
                try {
                    videoPlayer.pauseVideo()
                } catch (e: Exception) {
                    println("Error pausing video: ${e.message}")
                }
            }
        }

        fun isPlaying(): Boolean {
            return try {
                videoPlayer.isVideoPlaying()
            } catch (e: Exception) {
                println("Error checking if video is playing: ${e.message}")
                false
            }
        }

        fun seekToPosition(positionMs: Int) {
            try {
                videoPlayer.seekToPosition(positionMs)
            } catch (e: Exception) {
                println("Error seeking to position: ${e.message}")
            }
        }

        fun getCurrentPosition(): Int {
            return try {
                videoPlayer.getCurrentPosition()
            } catch (e: Exception) {
                println("Error getting current position: ${e.message}")
                0
            }
        }

        fun getDuration(): Int {
            return try {
                videoPlayer.getDuration()
            } catch (e: Exception) {
                println("Error getting duration: ${e.message}")
                0
            }
        }

        fun enterFullScreen() {
            try {
                videoPlayer.enterFullScreenMode()
            } catch (e: Exception) {
                println("Error entering full screen: ${e.message}")
            }
        }

        fun exitFullScreen() {
            try {
                videoPlayer.exitFullScreenMode()
            } catch (e: Exception) {
                println("Error exiting full screen: ${e.message}")
            }
        }



        fun showControls() {
            try {
                videoPlayer.showControlsManually()
            } catch (e: Exception) {
                println("Error showing controls: ${e.message}")
            }
        }

        fun hideControls() {
            try {
                videoPlayer.hideControlsManually()
            } catch (e: Exception) {
                println("Error hiding controls: ${e.message}")
            }
        }

        fun releasePlayer() {
            try {
                if (isPlayerInitialized) {
                    videoPlayer.onDestroy()
                    isPlayerInitialized = false
                    isVideoLoaded = false
                    currentVideoItem = null
                }
            } catch (e: Exception) {
                println("Error releasing player: ${e.message}")
                e.printStackTrace()
            }
        }

        fun onPause() {
            try {
                videoPlayer.onPause()
            } catch (e: Exception) {
                println("Error on pause: ${e.message}")
            }
        }

        fun onResume() {
            try {
                videoPlayer.onResume()
            } catch (e: Exception) {
                println("Error on resume: ${e.message}")
            }
        }

        fun handleBackPress(): Boolean {
            return try {
                videoPlayer.onBackPressed()
            } catch (e: Exception) {
                println("Error handling back press: ${e.message}")
                false
            }
        }
    }
}