# VideoPlayerLibrary

A lightweight, customizable video player library for Android applications built with Kotlin. This library provides essential video playback controls with a focus on simplicity and performance.

## 🚀 Features

- ⏩ Playback speed control (0.5X to 3X)
- 🔄 Screen orientation toggle (Portrait/Landscape)
- 🎮 Gesture controls:
  - Swipe up/down for volume control
  - Swipe left/right with two fingers for brightness adjustment
- 🎮 Basic UI with play/pause controls
- 🎬 Video title display
- 🔄 Full-screen mode support

## 📱 Requirements

- Android SDK 21+
- Kotlin 1.8+
- AndroidX libraries
- Minimum API Level 21 (Android 5.0)

## 📦 Installation

### Step 1: Add JitPack repository to your root build.gradle

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2: Add the dependency to your app's build.gradle

```gradle
dependencies {
    implementation 'com.github.yashraiyani098:VideoPlayerLibrary:1.0.0'
}
```

## 🛠 Usage

### Basic Implementation

1. Add the VideoPlayerLibrary to your layout:

```xml
<!-- In your layout XML -->
<com.ext.videoplayerlibrary.VideoPlayerLibrary
    android:id="@+id/videoPlayer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:vpl_primaryColor="@color/primary"
    app:vpl_secondaryColor="@color/secondary" />
```

2. Initialize and configure in your Activity/Fragment:

```kotlin
// In your Activity/Fragment
val videoPlayer = findViewById<VideoPlayerLibrary>(R.id.videoPlayer)

// Set video source
videoPlayer.setVideoUri(Uri.parse("your_video_url"))

// Set video title
videoPlayer.setVideoTitle("Your Video Title")
```

### Basic Controls

```kotlin
// Toggle play/pause
videoPlayer.togglePlayPause()

// Set playback speed
videoPlayer.setPlaybackSpeed(1.5f) // 1.5x speed

// Get current playback speed
val currentSpeed = videoPlayer.getCurrentSpeed()

// Toggle screen orientation
videoPlayer.toggleScreenOrientation()

// Enter/Exit full screen
videoPlayer.enterFullScreenMode()
videoPlayer.exitFullScreenMode()
```

### Gesture Controls

```kotlin
// Volume control (swipe up/down)
videoPlayer.adjustVolume(deltaY: Float)

// Brightness control (swipe left/right with two fingers)
videoPlayer.adjustBrightness(deltaY: Float)

// Show volume/brightness indicator
videoPlayer.showVolumeOrBrightnessIndicator(isVolume: Boolean, value: Int)
```

### Event Listeners

```kotlin
videoPlayer.setOnVideoPlayerListener(object : VideoPlayerLibrary.OnVideoPlayerListener {
    override fun onVideoPrepared(duration: Int) {
        // Video is ready to play
        Log.d("VideoPlayer", "Video duration: ${duration / 1000} seconds")
    }

    override fun onVideoStarted() {
        // Video started playing
        Log.d("VideoPlayer", "Video started playing")
    }

    override fun onVideoPaused() {
        // Video paused
        Log.d("VideoPlayer", "Video paused")
    }

    override fun onVideoCompleted() {
        // Video completed
        Log.d("VideoPlayer", "Video completed")
    }

    override fun onVideoError(what: Int, extra: Int) {
        // Error occurred
        Log.e("VideoPlayer", "Error: $what, Extra: $extra")
    }

    override fun onSpeedChanged(speed: Float) {
        // Playback speed changed
        Log.d("VideoPlayer", "Playback speed changed to $speed")
    }
})
```

## 🎨 Customization Options

```kotlin
// Customize colors
videoPlayer.apply {
    primaryColor = ContextCompat.getColor(context, R.color.your_primary_color)
    secondaryColor = ContextCompat.getColor(context, R.color.your_secondary_color)
}

// Configure gesture sensitivity
videoPlayer.apply {
    volumeStepSize = 0.1f // 10% volume change per swipe
    brightnessStepSize = 0.1f // 10% brightness change per swipe
}
```

## 📱 Device Compatibility

- Supports Android 5.0 (API 21) and above
- Optimized for both phones and tablets
- Adaptive to different screen sizes and orientations
- Optimized performance for smooth playback

## 📝 Best Practices

1. Always check for null before setting video URI:
```kotlin
if (videoUri != null) {
    videoPlayer.setVideoUri(videoUri)
}
```

2. Handle configuration changes properly:
```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    videoPlayer.onConfigurationChanged(newConfig)
}
```

3. Release resources when not needed:
```kotlin
override fun onDestroy() {
    videoPlayer.release()
    super.onDestroy()
}
```

## 📝 License

MIT License

Copyright (c) 2024 Yash Raiyani

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
