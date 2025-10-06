package com.saadho.funnutv.ui.gesture

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.saadho.funnutv.ui.layout.VideoLayoutManager

/**
 * Gesture handler for 4-direction scrolling in video feed
 */
class VideoGestureHandler(
    private val recyclerView: RecyclerView,
    private val layoutManager: VideoLayoutManager,
    private val onVideoChange: (direction: ScrollDirection) -> Unit
) : View.OnTouchListener {

    private val gestureDetector = GestureDetector(recyclerView.context, object : GestureDetector.SimpleOnGestureListener() {
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y
            val minVelocity = 1000f // Minimum velocity threshold
            val minDistance = 50f   // Minimum distance threshold
            
            // Check if the gesture meets minimum requirements
            if (Math.abs(velocityX) < minVelocity && Math.abs(velocityY) < minVelocity) {
                return false
            }
            
            if (Math.abs(deltaX) < minDistance && Math.abs(deltaY) < minDistance) {
                return false
            }
            
            // Determine scroll direction based on velocity and distance
            when {
                Math.abs(velocityY) > Math.abs(velocityX) && Math.abs(deltaY) > Math.abs(deltaX) -> {
                    // Vertical scrolling
                    if (deltaY > 0) {
                        android.util.Log.d("VideoGestureHandler", "UP gesture detected")
                        onVideoChange(ScrollDirection.UP)
                    } else {
                        android.util.Log.d("VideoGestureHandler", "DOWN gesture detected")
                        onVideoChange(ScrollDirection.DOWN)
                    }
                }
                Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(deltaX) > Math.abs(deltaY) -> {
                    // Horizontal scrolling
                    if (deltaX > 0) {
                        android.util.Log.d("VideoGestureHandler", "LEFT gesture detected")
                        onVideoChange(ScrollDirection.LEFT)
                    } else {
                        android.util.Log.d("VideoGestureHandler", "RIGHT gesture detected")
                        onVideoChange(ScrollDirection.RIGHT)
                    }
                }
            }
            
            return true
        }
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Handle single tap for play/pause
            return true
        }
        
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    })

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // Let the gesture detector handle the touch event
        val handled = gestureDetector.onTouchEvent(event)
        
        // If it's a horizontal gesture, consume the event to prevent RecyclerView scrolling
        if (handled && event.action == MotionEvent.ACTION_UP) {
            return true
        }
        
        return handled
    }

    enum class ScrollDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
