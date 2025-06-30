package com.ext.videoplayerlibrarytestingapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var currentVisiblePosition = 0

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

        setupRecyclerView()
        loadVideoData()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvVideos)
        videosAdapter = VideosAdapter()
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        recyclerView.apply {
            adapter = videosAdapter
            layoutManager = this@MainActivity.layoutManager
            setHasFixedSize(true)

            // Add PagerSnapHelper for Instagram-like scrolling
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)

            // Modified scroll listener - only track visible position, don't auto-play
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        // Only update the current visible position
                        val position = findVisibleVideoPosition()
                        if (position != -1) {
                            currentVisiblePosition = position
                            println("Current visible position: $position")
                        }
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // Optional: Pause videos that are no longer visible
                    pauseNonVisibleVideos()
                }
            })
        }
    }

    private fun pauseNonVisibleVideos() {
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        // Get current playing position from adapter
        val currentPlayingPosition = videosAdapter.getCurrentPlayingPosition()

        // Pause currently playing video if it's not in visible range
        if (currentPlayingPosition != -1 &&
            (currentPlayingPosition < firstVisiblePosition || currentPlayingPosition > lastVisiblePosition)) {
            videosAdapter.pauseAllVideos()
        }
    }

    private fun findVisibleVideoPosition(): Int {
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        // Find the item that is most visible (center of screen)
        var maxVisibleHeight = 0
        var mostVisiblePosition = -1

        for (i in firstVisiblePosition..lastVisiblePosition) {
            val view = layoutManager.findViewByPosition(i)
            view?.let {
                val visibleHeight = getVisibleHeight(it)
                if (visibleHeight > maxVisibleHeight) {
                    maxVisibleHeight = visibleHeight
                    mostVisiblePosition = i
                }
            }
        }

        return mostVisiblePosition
    }

    private fun getVisibleHeight(view: android.view.View): Int {
        val recyclerViewTop = recyclerView.top
        val recyclerViewBottom = recyclerView.bottom
        val viewTop = view.top
        val viewBottom = view.bottom

        val visibleTop = maxOf(recyclerViewTop, viewTop)
        val visibleBottom = minOf(recyclerViewBottom, viewBottom)

        return maxOf(0, visibleBottom - visibleTop)
    }

    private fun loadVideoData() {
        // Create sample video data
        val videoList = mutableListOf<VideoItem>()

        try {
            // First, try to add videos from raw resources (if they exist)
            val rawVideoResources = listOf(
                Pair(R.raw.sample_video, "Sample Video 1"),
            )

            rawVideoResources.forEach { (resourceId, title) ->
                try {
                    videoList.add(
                        VideoItem(
                            videoUri = Uri.parse("android.resource://$packageName/$resourceId"),
                            title = title,
                            qualityOptions = arrayListOf("Auto", "1080p", "720p", "480p", "360p"),
                            subtitleOptions = arrayListOf("None", "English", "Spanish", "French")
                        )
                    )
                } catch (e: Exception) {
                    println("Resource $title not found in raw resources")
                }
            }

            // Add external sample videos (these are publicly available test videos)
            val externalVideos = listOf(
                VideoItem(
                    videoUri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
                    title = "Big Buck Bunny",
                    qualityOptions = arrayListOf("Auto", "1080p", "720p", "480p"),
                    subtitleOptions = arrayListOf("None", "English")
                ),
                VideoItem(
                    videoUri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
                    title = "Tears of Steel",
                    qualityOptions = arrayListOf("Auto", "1080p", "720p", "480p"),
                    subtitleOptions = arrayListOf("None", "English", "Spanish", "French")
                )
            )

            videoList.addAll(externalVideos)

        } catch (e: Exception) {
            println("Error loading video data: ${e.message}")
            e.printStackTrace()
        }

        if (videoList.isNotEmpty()) {
            videosAdapter.setVideoList(videoList)
            println("Loaded ${videoList.size} videos successfully")

            // Don't auto-play any video - wait for user interaction
            currentVisiblePosition = 0
        } else {
            println("No videos to load")
        }
    }

    override fun onPause() {
        super.onPause()
        // Use the actual adapter method for activity pause
        videosAdapter.onActivityPause()
    }

    override fun onResume() {
        super.onResume()
        // Use the actual adapter method for activity resume
        videosAdapter.onActivityResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release all video players
        videosAdapter.releaseAllPlayers()
    }

    override fun onBackPressed() {
        // Handle back press through adapter (for fullscreen exit, etc.)
        if (!videosAdapter.handleBackPress()) {
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        // Handle PiP mode changes for current video
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // The video player library should handle PiP mode internally
            // through the OnVideoPlayerListener.onPictureInPictureModeChanged callback
        }
    }

    // Helper methods for external control
    fun getCurrentVideoPosition(): Int {
        return currentVisiblePosition
    }

    fun getCurrentPlayingPosition(): Int {
        return videosAdapter.getCurrentPlayingPosition()
    }

    // Method to get current visible video view holder for manual control
    fun getCurrentVisibleVideoHolder(): VideosAdapter.VideoViewHolder? {
        return recyclerView.findViewHolderForAdapterPosition(currentVisiblePosition) as? VideosAdapter.VideoViewHolder
    }

    // Method to get currently playing video view holder
    fun getCurrentPlayingVideoHolder(): VideosAdapter.VideoViewHolder? {
        val playingPosition = videosAdapter.getCurrentPlayingPosition()
        return if (playingPosition != -1) {
            recyclerView.findViewHolderForAdapterPosition(playingPosition) as? VideosAdapter.VideoViewHolder
        } else null
    }

    fun addVideo(videoItem: VideoItem) {
        videosAdapter.addVideo(videoItem)
    }

    fun pauseAllVideos() {
        videosAdapter.pauseAllVideos()
    }

    // Utility method to play video at specific position
    fun playVideoAt(position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideosAdapter.VideoViewHolder
        viewHolder?.playVideo()
    }

    // Utility method to pause video at specific position
    fun pauseVideoAt(position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? VideosAdapter.VideoViewHolder
        viewHolder?.pauseVideo()
    }
}