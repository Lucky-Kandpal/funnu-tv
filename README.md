# FunnuTV - Advanced Android Video Feed App

A sophisticated TikTok/Reels-style video feed application built with Kotlin and XML, featuring seamless auto-playing MP4 videos with 4-direction scrolling, intelligent caching, and a beautiful pink gradient theme.

## 🎯 Key Features

### 🎬 Video Experience
- **Seamless Auto-play**: Videos automatically start playing when scrolled into view
- **Vertical-Direction Scrolling**: Intuitive navigation - scroll up, down, left, or right to change videos
- **Smooth Transitions**: PagerSnapHelper ensures smooth video-to-video transitions
- **Landscape-Only Mode**: Strictly landscape orientation, optimized for video consumption
- **Full-Screen Immersive**: Hidden system UI for distraction-free viewing

### 🧠 Intelligent Caching System
- **Adaptive Cache Management**: Automatically adjusts cache size based on available storage
- **Smart Preloading**: Preloads next 3 videos for seamless playback
- **LRU Eviction Policy**: Efficient memory management with Least Recently Used algorithm
- **System Capacity Analysis**: Analyzes device storage and optimizes cache accordingly
- **Background Video Preparation**: Videos are prepared on background threads for better performance

### 🎨 Beautiful Design
- **Pink Gradient Theme**: Elegant soft pink gradient background (#FFC0CB → #FF69B4)
- **Curvy Video Borders**: Rounded corners with pink borders for modern aesthetics
- **Smooth Animations**: 60fps smooth scrolling and playback
- **Loading Indicators**: Visual feedback during video buffering
- **Error Handling**: Graceful error recovery with auto-advance to next video

### 🔧 Advanced Architecture
- **MVVM Pattern**: Clean separation of concerns with ViewModel and Repository
- **Single ExoPlayer Instance**: Memory-efficient video playback management
- **Custom Layout Manager**: Optimized for video feed performance
- **Gesture Recognition**: Advanced touch gesture handling for intuitive navigation
- **Lifecycle Management**: Proper video pause/resume on app state changes

## 📱 Technical Specifications

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36 (Android 15+)
- **Version**: 1.5 (Build 8)
- **Orientation**: Landscape only
- **Network**: Internet connection required for video streaming
- **Architecture**: MVVM with Repository pattern

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+
- Device/Emulator with landscape support

### Installation
1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/FunnuTV.git
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the project folder
   - Wait for Gradle sync to complete

3. **Build and Run**
   - Connect your Android device or start an emulator
   - Click the "Run" button or use `Ctrl+R`
   - The app will install and launch in landscape mode

## 📁 Project Structure

```
app/src/main/java/com/saadho/funnutv/
├── adapter/
│   └── VideoAdapter.kt              # RecyclerView adapter with video lifecycle management
├── cache/
│   ├── CacheManager.kt              # Core ExoPlayer cache management
│   ├── SystemCapacityAnalyzer.kt    # Device storage analysis
│   ├── VideoCacheManager.kt         # Video-specific cache operations
│   └── VideoPreloader.kt            # Intelligent video preloading
├── data/
│   ├── Video.kt                     # Video data model
│   ├── VideoRepository.kt           # Data repository with network handling
│   └── VideoResponse.kt             # JSON response wrapper
├── player/
│   └── ExoPlayerPool.kt             # Singleton ExoPlayer pool management
├── ui/
│   ├── activities/
│   │   └── MainActivity.kt          # Main activity with landscape enforcement
│   ├── gesture/
│   │   └── VideoGestureHandler.kt   # 4-direction gesture recognition
│   └── layout/
│       └── VideoLayoutManager.kt    # Custom RecyclerView layout manager
├── viewmodel/
│   └── VideoViewModel.kt            # ViewModel for video data and state
├── NetworkUtils.kt                  # Network connectivity utilities
└── funnuTVApp.kt                    # Application class with initialization
```

## 🎨 Design System

### Color Palette
- **Primary**: #FF69B4 (Hot Pink)
- **Accent**: #D81B60 (Deep Pink)
- **Background**: Pink gradient (#FFC0CB → #FF69B4)
- **Text**: White for optimal contrast
- **Borders**: Curvy pink borders with rounded corners

### UI Components
- **Video Player**: Full-screen with rounded corners
- **Loading States**: Smooth progress indicators
- **Error States**: User-friendly error messages with retry options
- **Network States**: No internet connection handling with animations

## 🔧 Core Components

### ExoPlayerPool
- **Singleton Pattern**: Single active player instance for memory efficiency
- **Background Preparation**: Videos prepared on background threads
- **Auto-play Support**: Seamless video transitions with automatic playback
- **Error Recovery**: Robust error handling and recovery mechanisms

### Intelligent Caching System
- **CacheManager**: Core caching infrastructure with adaptive sizing
- **VideoCacheManager**: Video-specific cache operations and access tracking
- **VideoPreloader**: Smart preloading of upcoming videos
- **SystemCapacityAnalyzer**: Device storage analysis and optimization

### VideoGestureHandler
- **4-Direction Detection**: Up, down, left, right scroll recognition
- **Smooth Gestures**: Natural gesture recognition with proper thresholds
- **Performance Optimized**: Efficient gesture processing for smooth UX

### VideoLayoutManager
- **Custom Implementation**: Optimized for video feed performance
- **Snap Behavior**: Ensures videos are properly centered
- **Memory Efficient**: Proper view recycling and lifecycle management

## 📊 Performance Optimizations

### Memory Management
- **Single ExoPlayer**: Reduces memory footprint significantly
- **View Recycling**: Efficient RecyclerView implementation
- **Smart Caching**: Adaptive cache size with intelligent eviction
- **Background Processing**: Video preparation on background threads

### Network Optimization
- **Intelligent Preloading**: Only preloads when network is available
- **Cache-First Strategy**: Prioritizes cached content for faster loading
- **Connection Monitoring**: Real-time network state awareness
- **Error Recovery**: Automatic retry mechanisms for failed requests

### UI Performance
- **60fps Scrolling**: Smooth video transitions and scrolling
- **Hardware Acceleration**: Leverages GPU for video rendering
- **Efficient Layouts**: Optimized XML layouts for performance
- **Lifecycle Awareness**: Proper pause/resume handling

## 🌐 Video Sources & Content

The app includes a curated collection of high-quality test videos:
- **Big Buck Bunny**: Classic open-source animation
- **Elephant's Dream**: Blender Foundation animation
- **Various Demo Videos**: For comprehensive testing and demonstration

## 🔒 Permissions & Security

### Required Permissions
- `INTERNET`: Essential for video streaming
- `ACCESS_NETWORK_STATE`: Network connectivity monitoring
- `ACCESS_WIFI_STATE`: WiFi state detection
- `CHANGE_WIFI_STATE`: Network optimization

### Security Features
- **Network Security Config**: Secure network communication
- **ProGuard Obfuscation**: Code protection in release builds
- **Firebase Integration**: Secure backend services (optional)

## 📱 Android 15+ Compatibility

### Modern Android Features
- **Predictive Back**: Properly handled with smooth transitions
- **Multi-window Support**: Landscape enforcement maintained
- **Freeform Mode**: Orientation locked to landscape
- **Configuration Changes**: Handled without layout recreation
- **Edge-to-Edge**: Full immersive experience

### Performance Optimizations
- **Target SDK 36**: Latest Android optimizations
- **NDK 29.0.14033849**: Latest native development tools
- **Build Tools 36.0.0**: Latest build system features

## 🛠️ Dependencies & Libraries

### Core Dependencies
- **ExoPlayer 1.8.0**: Professional video playback engine
- **RecyclerView 1.4.0**: Efficient list management
- **Lifecycle 2.9.4**: MVVM architecture support
- **Material Design 1.13.0**: Modern UI components

### Additional Libraries
- **Gson 2.13.2**: JSON parsing and serialization
- **Glide 5.0.5**: Image loading and caching
- **Firebase BOM 34.3.0**: Backend services integration
- **Coroutines Play Services 1.10.2**: Asynchronous operations

## 🔄 Recent Updates (v1.5)

### Bug Fixes
- **Fixed Auto-play During Scrolling**: Videos now automatically play when scrolled into view
- **Improved Error Handling**: Better recovery from network and playback errors
- **Enhanced Caching**: More intelligent cache management and preloading

### Performance Improvements
- **Background Video Preparation**: Faster video loading with background processing
- **Memory Optimization**: Reduced memory usage through better resource management
- **Smoother Scrolling**: Enhanced gesture recognition and scroll performance

### UI/UX Enhancements
- **Curvy Video Borders**: Added rounded corners with pink borders
- **Better Loading States**: Improved visual feedback during buffering
- **Network State Handling**: Enhanced no-internet experience with animations

## 🧪 Testing

### Test Coverage
- **Unit Tests**: Core business logic testing
- **Integration Tests**: Video playback and caching testing
- **UI Tests**: User interaction and gesture testing

### Test Devices
- **Minimum**: Android 7.0 (API 24)
- **Recommended**: Android 10+ for optimal performance
- **Orientations**: Landscape only

## 🚀 Building for Production

### Release Build
```bash
./gradlew assembleRelease
```

### Key Release Features
- **ProGuard Enabled**: Code obfuscation and optimization
- **Resource Shrinking**: Reduced APK size
- **Firebase Integration**: Optional backend services
- **Optimized Performance**: Release-specific optimizations

## 📝 License

This project is created for educational and demonstration purposes. Feel free to use and modify according to your needs.

## 🤝 Contributing

We welcome contributions! Please feel free to:
- Submit issues and bug reports
- Propose new features and enhancements
- Submit pull requests with improvements
- Share feedback and suggestions

### Development Guidelines
- Follow Kotlin coding conventions
- Maintain MVVM architecture patterns
- Add appropriate tests for new features
- Update documentation for significant changes

## 📞 Support

For support, questions, or feedback:
- Create an issue on GitHub
- Check the documentation
- Review the code comments for implementation details

---

**Built with ❤️ using Kotlin, XML, and modern Android development practices**

*FunnuTV - Where videos come to life with seamless scrolling and intelligent caching*