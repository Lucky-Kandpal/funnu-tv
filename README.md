# Funnu TV - Android Video Feed App

A TikTok/Reels-style video feed app built with Kotlin and XML, featuring auto-playing MP4 videos with 4-direction scrolling and a beautiful pink gradient theme.

## 🎯 Features

- **Auto-playing Video Feed**: Seamless video playback with automatic transitions
- **4-Direction Scrolling**: Scroll up, down, left, or right to change videos
- **Landscape-Only Mode**: Strictly landscape orientation, even on Android 15+
- **Pink Gradient Theme**: Beautiful soft pink gradient background (#FFC0CB → #FF69B4)
- **Memory Efficient**: Single ExoPlayer instance with smart caching
- **Adaptive Caching**: 10% of available storage (max 500MB) with LRU eviction
- **Smooth Performance**: 60fps smooth scrolling and playback

## 🏗️ Architecture

- **MVVM Pattern**: Clean separation of concerns with ViewModel and Repository
- **ExoPlayer Integration**: Professional video playback with caching
- **Custom Layout Manager**: Supports 4-direction scrolling
- **Gesture Handling**: Touch gestures for intuitive navigation
- **Lifecycle Management**: Proper video pause/resume on app state changes

## 📱 Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36 (Android 15+)
- **Orientation**: Landscape only
- **Network**: Internet connection required for video streaming

## 🚀 Getting Started

1. **Clone the repository**
2. **Open in Android Studio**
3. **Sync Gradle dependencies**
4. **Build and run on device/emulator**

## 📁 Project Structure

```
app/src/main/java/com/saadho/funnutv/
├── adapter/
│   └── VideoAdapter.kt              # RecyclerView adapter for video items
├── cache/
│   └── CacheManager.kt              # ExoPlayer cache management
├── data/
│   ├── Video.kt                     # Video data model
│   ├── VideoRepository.kt           # Data repository
│   └── VideoResponse.kt             # JSON response wrapper
├── player/
│   └── ExoPlayerPool.kt             # ExoPlayer singleton pool
├── ui/
│   ├── activities/
│   │   └── MainActivity.kt          # Main activity with landscape handling
│   ├── gesture/
│   │   └── VideoGestureHandler.kt   # 4-direction gesture handling
│   └── layout/
│       └── VideoLayoutManager.kt    # Custom layout manager
├── viewmodel/
│   └── VideoViewModel.kt            # ViewModel for video data
└── funnuTVApp.kt                    # Application class
```

## 🎨 Theme & Design

- **Primary Color**: #FF69B4 (Hot Pink)
- **Accent Color**: #D81B60 (Deep Pink)
- **Background**: Pink gradient (#FFC0CB → #FF69B4)
- **Text**: White for contrast
- **Full Screen**: Immersive experience with hidden system UI

## 🔧 Key Components

### ExoPlayerPool
- Singleton pattern for efficient resource management
- Single active player instance
- Background preloading for seamless transitions

### CacheManager
- Adaptive cache sizing based on available storage
- LRU eviction policy
- Automatic cleanup and management

### VideoGestureHandler
- 4-direction scroll detection
- Smooth gesture recognition
- Intuitive navigation controls

### VideoLayoutManager
- Custom RecyclerView layout manager
- Support for both vertical and horizontal scrolling
- Optimized for video feed performance

## 📊 Performance Optimizations

- **Single ExoPlayer**: Reduces memory usage
- **View Recycling**: Efficient RecyclerView implementation
- **Smart Caching**: Adaptive cache size with LRU eviction
- **Background Preloading**: Next video preparation
- **Lifecycle Management**: Proper pause/resume handling

## 🌐 Video Sources

The app includes sample videos from Google's test video collection:
- Big Buck Bunny
- Elephant's Dream
- Various demo videos for testing

## 🔒 Permissions

- `INTERNET`: Required for video streaming
- `ACCESS_NETWORK_STATE`: For network connectivity checks

## 📱 Android 15+ Compatibility

- **Predictive Back**: Properly handled
- **Multi-window**: Landscape enforcement maintained
- **Freeform Mode**: Orientation locked to landscape
- **Configuration Changes**: Handled without layout recreation

## 🛠️ Dependencies

- **ExoPlayer 2.19.1**: Video playback
- **RecyclerView 1.3.2**: Video feed display
- **Lifecycle 2.8.4**: MVVM architecture
- **Gson 2.10.1**: JSON parsing

## 📝 License

This project is created for educational and demonstration purposes.

## 🤝 Contributing

Feel free to submit issues and enhancement requests!

---

**Built with ❤️ using Kotlin and XML**
