package com.saadho.funnutv.ui.layout

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom layout manager that supports 4-direction scrolling
 * Extends LinearLayoutManager to handle both vertical and horizontal scrolling
 */
class VideoLayoutManager(
    context: Context,
    orientation: Int = VERTICAL,
    reverseLayout: Boolean = false
) : LinearLayoutManager(context, orientation, reverseLayout) {

    companion object {
        const val VERTICAL = RecyclerView.VERTICAL
        const val HORIZONTAL = RecyclerView.HORIZONTAL
    }

    private var isVertical = orientation == VERTICAL

    /**
     * Switch between vertical and horizontal orientation
     */
    override fun setOrientation(orientation: Int) {
        isVertical = orientation == VERTICAL
        super.setOrientation(orientation)
    }

    /**
     * Find the first completely visible item position
     */
    override fun findFirstCompletelyVisibleItemPosition(): Int {
        return if (isVertical) {
            super.findFirstCompletelyVisibleItemPosition()
        } else {
            // For horizontal scrolling, find the leftmost completely visible item
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != null) {
                    val position = getPosition(child)
                    if (isViewCompletelyVisible(child, true)) {
                        return position
                    }
                }
            }
            RecyclerView.NO_POSITION
        }
    }

    /**
     * Find the last completely visible item position
     */
    override fun findLastCompletelyVisibleItemPosition(): Int {
        return if (isVertical) {
            super.findLastCompletelyVisibleItemPosition()
        } else {
            // For horizontal scrolling, find the rightmost completely visible item
            val childCount = childCount
            for (i in childCount - 1 downTo 0) {
                val child = getChildAt(i)
                if (child != null) {
                    val position = getPosition(child)
                    if (isViewCompletelyVisible(child, true)) {
                        return position
                    }
                }
            }
            RecyclerView.NO_POSITION
        }
    }

    /**
     * Check if a view is completely visible
     */
    private fun isViewCompletelyVisible(child: View, acceptEndPointInclusion: Boolean): Boolean {
        val bounds = Rect()
        getDecoratedBoundsWithMargins(child, bounds)
        return if (isVertical) {
            val top = bounds.top
            val bottom = bounds.bottom
            val height = height
            val topBound = if (acceptEndPointInclusion) 0 else paddingTop
            val bottomBound = if (acceptEndPointInclusion) height else height - paddingBottom
            
            top >= topBound && bottom <= bottomBound
        } else {
            val left = bounds.left
            val right = bounds.right
            val width = width
            val leftBound = if (acceptEndPointInclusion) 0 else paddingLeft
            val rightBound = if (acceptEndPointInclusion) width else width - paddingRight
            
            left >= leftBound && right <= rightBound
        }
    }
}
