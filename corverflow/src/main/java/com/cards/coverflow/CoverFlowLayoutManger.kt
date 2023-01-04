package com.cards.coverflow

import android.animation.Animator
import androidx.recyclerview.widget.RecyclerView
import android.util.SparseArray
import android.util.SparseBooleanArray
import androidx.recyclerview.widget.RecyclerView.Recycler
import android.animation.ValueAnimator
import androidx.recyclerview.widget.OrientationHelper
import android.view.ViewGroup
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import kotlin.jvm.JvmOverloads
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import java.lang.IllegalArgumentException

class CoverFlowLayoutManger private constructor(
    isFlat: Boolean, isGreyItem: Boolean,
    isAlphaItem: Boolean, cstInterval: Float,
    isLoop: Boolean, is3DItem: Boolean, orientation: Int
) : RecyclerView.LayoutManager() {
    private var mOffsetAll = 0
    private var mDecoratedChildWidth = 0
    private var mDecoratedChildHeight = 0
    private var mIntervalRatio = 0.5f
    private var mStartX = 0
    private var mStartY = 0
    private val mAllItemFrames = SparseArray<Rect>()
    private val mHasAttachedItems = SparseBooleanArray()
    private var mRecycle: Recycler? = null
    private var mState: RecyclerView.State? = null
    private var mAnimation: ValueAnimator? = null
    var selectedPos = 0
        private set
    private var mSelectPositionNature = 0
    private var mLastSelectPosition = 0
    private var mSelectedListener: OnSelected? = null
    private var mIsFlatFlow = false
    private var mItemGradualGrey = false
    private var mItemGradualAlpha = false
    private var mIsLoop = false
    private var enableGesture = true
    private var orientation = OrientationHelper.VERTICAL
    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val isVertical: Boolean
        get() = orientation == OrientationHelper.VERTICAL
    val isHorizontal: Boolean
        get() = orientation == OrientationHelper.HORIZONTAL

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (itemCount <= 0 || state.isPreLayout) {
            mOffsetAll = 0
            return
        }
        mAllItemFrames.clear()
        mHasAttachedItems.clear()
        val scrap = recycler.getViewForPosition(0)
        addView(scrap)
        measureChildWithMargins(scrap, 0, 0)
        mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap)
        mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap)
        mStartX = Math.round((horizontalSpace - mDecoratedChildWidth) * 1.0f / 2)
        mStartY = Math.round((verticalSpace - mDecoratedChildHeight) * 1.0f / 2)
        var offset = if (isVertical) mStartY.toFloat() else mStartX.toFloat()
        var i = 0
        while (i < itemCount && i < MAX_RECT_COUNT) {
            var frame = mAllItemFrames[i]
            if (frame == null) {
                frame = Rect()
            }
            if (isVertical) {
                frame[mStartX, Math.round(offset), mStartX + mDecoratedChildWidth] =
                    Math.round(offset + mDecoratedChildHeight)
            } else {
                frame[Math.round(offset), mStartY, Math.round(offset + mDecoratedChildWidth)] =
                    mStartY + mDecoratedChildHeight
            }
            mAllItemFrames.put(i, frame)
            mHasAttachedItems.put(i, false)
            offset = offset + intervalDistance
            i++
        }
        detachAndScrapAttachedViews(recycler)
        if ((mRecycle == null || mState == null) &&
            selectedPos != 0
        ) {
            mOffsetAll = calculateOffsetForPosition(selectedPos)
            onSelectedCallBack()
        }
        layoutItems(recycler, state, SCROLL_TO_START)
        mRecycle = recycler
        mState = state
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: RecyclerView.State): Int {
        return if (isVertical) {
            scrollByGesture(dy, recycler, state)
        } else super.scrollVerticallyBy(dy, recycler, state)
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: RecyclerView.State): Int {
        return if (isHorizontal) {
            scrollByGesture(dx, recycler, state)
        } else super.scrollHorizontallyBy(dx, recycler, state)
    }

    private fun scrollByGesture(
        transDistance: Int,
        recycler: Recycler,
        state: RecyclerView.State
    ): Int {
        if (mAnimation != null && mAnimation!!.isRunning) {
            mAnimation!!.cancel()
        }
        var travel = transDistance
        if (!mIsLoop) {
            if (transDistance + mOffsetAll < 0) {
                travel = -mOffsetAll
            } else if (transDistance + mOffsetAll > maxOffset) {
                travel = (maxOffset - mOffsetAll).toInt()
            }
        }
        mOffsetAll += travel
        layoutItems(recycler, state, if (transDistance > 0) SCROLL_TO_START else SCROLL_TO_END)
        return travel
    }

    private fun layoutItems(
        recycler: Recycler?,
        state: RecyclerView.State?, scrollDirection: Int
    ) {
        if (state == null || state.isPreLayout) {
            return
        }
        var displayFrame: Rect? = null
        displayFrame = if (isVertical) {
            Rect(0, mOffsetAll, horizontalSpace, mOffsetAll + verticalSpace)
        } else {
            Rect(mOffsetAll, 0, mOffsetAll + horizontalSpace, verticalSpace)
        }
        var position = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if(child != null) {
                position = if (child.tag != null) {
                    val tag = checkTag(child.tag)
                    tag!!.pos
                } else {
                    getPosition(child)
                }
                val rect = getFrame(position)
                if (!Rect.intersects(displayFrame, rect)) {
                    removeAndRecycleView(child, recycler!!)
                    mHasAttachedItems.delete(position)
                } else {
                    layoutItem(child, rect)
                    mHasAttachedItems.put(position, true)
                }
            }
        }
        if (position == 0) {
            position = centerPosition
        }
        var min = position - 20
        var max = position + 20
        if (!mIsLoop) {
            if (min < 0) {
                min = 0
            }
            if (max > itemCount) {
                max = itemCount
            }
        }
        for (i in min until max) {
            val rect = getFrame(i)
            if (Rect.intersects(displayFrame, rect) &&
                !mHasAttachedItems[i]
            ) {
                var actualPos = i % itemCount
                if (actualPos < 0) {
                    actualPos = itemCount + actualPos
                }
                val scrap = recycler!!.getViewForPosition(actualPos)
                checkTag(scrap.tag)
                scrap.tag = TAG(i)
                measureChildWithMargins(scrap, 0, 0)
                if (scrollDirection == SCROLL_TO_END) {
                    addView(scrap, 0)
                } else {
                    addView(scrap)
                }
                layoutItem(scrap, rect)
                mHasAttachedItems.put(i, true)
            }
        }
    }

    private fun layoutItem(child: View?, frame: Rect) {
        if (isVertical) {
            layoutDecorated(
                child!!,
                frame.left,
                frame.top - mOffsetAll,
                frame.right,
                frame.bottom - mOffsetAll
            )
        } else {
            layoutDecorated(
                child!!,
                frame.left - mOffsetAll,
                frame.top,
                frame.right - mOffsetAll,
                frame.bottom
            )
        }
        if (!mIsFlatFlow) {
            child.scaleX = computeScale(frame)
            child.scaleY = computeScale(frame)
        }
        if (mItemGradualAlpha) {
            child.alpha = computeAlpha(frame)
        }
        if (mItemGradualGrey) {
            greyItem(child, frame)
        }
    }

    private fun getFrame(index: Int): Rect {
        var frame = mAllItemFrames[index]
        if (frame == null) {
            if (isVertical) {
                //纵向
                frame = Rect()
                val offset = (mStartY + intervalDistance * index).toFloat() //原始位置累加（即累计间隔距离）
                frame[mStartX, Math.round(offset), mStartX + mDecoratedChildWidth] =
                    Math.round(offset + mDecoratedChildHeight)
            } else {
                //横向
                frame = Rect()
                val offset = (mStartX + intervalDistance * index).toFloat() //原始位置累加（即累计间隔距离）
                frame[Math.round(offset), mStartY, Math.round(offset + mDecoratedChildWidth)] =
                    mStartY + mDecoratedChildHeight
            }
        }
        return frame
    }

    private fun greyItem(child: View?, frame: Rect) {
        var value = 1f
        value = if (isHorizontal) {
            computeGreyScale(frame.left - mOffsetAll)
        } else {
            computeGreyScale(frame.top - mOffsetAll)
        }
        val cm = ColorMatrix(
            floatArrayOf(
                value,
                0f,
                0f,
                0f,
                120 * (1 - value),
                0f,
                value,
                0f,
                0f,
                120 * (1 - value),
                0f,
                0f,
                value,
                0f,
                120 * (1 - value),
                0f,
                0f,
                0f,
                1f,
                250 * (1 - value)
            )
        )
        //cm.setSaturation(0.9f);

        // Create a paint object with color matrix
        val greyPaint = Paint()
        greyPaint.colorFilter = ColorMatrixColorFilter(cm)

        // Create a hardware layer with the grey paint
        child!!.setLayerType(View.LAYER_TYPE_HARDWARE, greyPaint)
        if (value >= 1) {
            // Remove the hardware layer
            child.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        when (state) {
            RecyclerView.SCROLL_STATE_IDLE -> fixOffsetWhenFinishScroll()
            RecyclerView.SCROLL_STATE_DRAGGING -> if (mSelectedListener != null) {
                mSelectedListener!!.onItemSelectStart()
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {}
            else -> {}
        }
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position > itemCount - 1) {
            return
        }
        mOffsetAll = calculateOffsetForPosition(position)
        if (mRecycle == null || mState == null) {
            selectedPos = position
        } else {
            layoutItems(
                mRecycle,
                mState,
                if (position > selectedPos) SCROLL_TO_START else SCROLL_TO_END
            )
            onSelectedCallBack()
            if (mSelectedListener != null) {
                mSelectedListener!!.onItemSelectEnd()
            }
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val finalOffset = calculateOffsetForPosition(position)
        if (mRecycle == null || mState == null) {
            selectedPos = position
        } else {
            startScroll(mOffsetAll, finalOffset, smoothScrollDuration)
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return if (isHorizontal) {
            enableGesture
        } else false
    }

    override fun canScrollVertically(): Boolean {
        return if (isVertical) {
            enableGesture
        } else false
    }

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?
    ) {
        removeAllViews()
        mRecycle = null
        mState = null
        mOffsetAll = 0
        selectedPos = 0
        mLastSelectPosition = 0
        mHasAttachedItems.clear()
        mAllItemFrames.clear()
    }

    private val horizontalSpace: Int
        private get() = width - paddingRight - paddingLeft
    private val verticalSpace: Int
        private get() = height - paddingBottom - paddingTop
    private val maxOffset: Float
        private get() = ((itemCount - 1) * intervalDistance).toFloat()

    private fun computeScale(r: Rect?): Float {
        if (r == null) {
            return 1f
        }
        var scale = 0f
        scale = if (isHorizontal) {
            1 - Math.abs(r.left - mOffsetAll - mStartX) * 1.0f / Math.abs(mStartX + mDecoratedChildWidth / mIntervalRatio)
        } else {
            1 - Math.abs(r.top - mOffsetAll - mStartY) * 1.0f / Math.abs(mStartY + mDecoratedChildHeight / mIntervalRatio)
        }
        if (scale < 0) {
            scale = 0f
        }
        if (scale > 1) {
            scale = 1f
        }
        return scale
    }

    private fun computeGreyScale(x: Int): Float {
        return if (isHorizontal) {
            val itemMidPos = x + mDecoratedChildWidth / 2f //item中点x坐标
            val itemDx2Mid = Math.abs(itemMidPos - horizontalSpace / 2f) //item中点距离控件中点距离
            var value = 1 - itemDx2Mid * 1.0f / (horizontalSpace / 2f)
            if (value < 0.1) {
                value = 0.1f
            }
            if (value > 1) {
                value = 1f
            }
            value = Math.pow(value.toDouble(), .8).toFloat()
            value
        } else {
            val itemMidPos = x + mDecoratedChildHeight / 2f //item中点x坐标
            val itemDx2Mid = Math.abs(itemMidPos - verticalSpace / 2f) //item中点距离控件中点距离
            var value = 1 - itemDx2Mid * 1.0f / (verticalSpace / 2f)
            if (value < 0.1) {
                value = 0.1f
            }
            if (value > 1) {
                value = 1f
            }
            value = Math.pow(value.toDouble(), .8).toFloat()
            value
        }
    }

    private fun computeAlpha(r: Rect?): Float {
        if (r == null) {
            return 1f
        }
        var alpha = 1f
        alpha = if (isHorizontal) {
            1 - Math.abs(r.left - mOffsetAll - mStartX) * 1.0f / Math.abs(mStartX + mDecoratedChildWidth / mIntervalRatio)
        } else {
            1 - Math.abs(r.top - mOffsetAll - mStartY) * 1.0f / Math.abs(mStartY + mDecoratedChildHeight / mIntervalRatio)
        }
        if (alpha < 0.3f) {
            alpha = 0.3f
        }
        if (alpha > 1) {
            alpha = 1.0f
        }
        return alpha
    }

    private fun calculateOffsetForPosition(position: Int): Int {
        return Math.round((intervalDistance * position).toFloat())
    }

    private fun fixOffsetWhenFinishScroll() {
        if (intervalDistance != 0) { // 判断非 0 ，否则除 0 会导致异常
            var scrollN = (mOffsetAll * 1.0f / intervalDistance).toInt()
            val moreDx = (mOffsetAll % intervalDistance).toFloat()
            if (Math.abs(moreDx) > intervalDistance * 0.5) {
                if (moreDx > 0) {
                    scrollN++
                } else {
                    scrollN--
                }
            }
            val finalOffset = scrollN * intervalDistance
            startScroll(mOffsetAll, finalOffset, smoothScrollDuration)
            selectedPos = Math.abs(Math.round(finalOffset * 1.0f / intervalDistance)) % itemCount
        } else {
            if (mSelectedListener != null) {
                mSelectedListener!!.onItemSelectEnd()
            }
        }
    }

    @JvmOverloads
    fun randomSmoothScrollToPosition(duration: Long = 4000L): Boolean {
        val random = (Math.random() * 50).toInt()
        //圈数总个数
        val lapCount = 50
        val pos = mSelectPositionNature + lapCount + random
        return randomSmoothScrollToPosition(duration, pos)
    }

    fun randomSmoothScrollToPosition(duration: Long, pos: Int): Boolean {
        //在所有项目中随机一个
        if (enableGesture) {
            val offset = calculateOffsetForPosition(pos)
            startScroll(mOffsetAll, offset, duration, AccelerateDecelerateInterpolator(), true)
            return true
        }
        return false
    }

    private fun startScroll(
        from: Int,
        to: Int,
        duration: Long,
        interpolator: Interpolator? = null,
        lockGesture: Boolean = false
    ) {
        if (mAnimation != null && mAnimation!!.isRunning) {
            mAnimation!!.cancel()
        }
        val direction = if (from < to) SCROLL_TO_START else SCROLL_TO_END
        mAnimation = ValueAnimator.ofFloat(from.toFloat(), to.toFloat())
        mAnimation?.duration = if (duration <= 0) 500 else duration
        mAnimation?.interpolator = interpolator ?: DecelerateInterpolator()
        mAnimation?.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
            if (!destoryed) {
                mOffsetAll = Math.round(animation.animatedValue as Float)
                layoutItems(mRecycle, mState, direction)
            }
        })
        mAnimation?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (lockGesture) {
                    enableGesture = false
                }
                if (mSelectedListener != null) {
                    mSelectedListener!!.onItemSelectStart()
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                onSelectedCallBack()
                if (mSelectedListener != null) {
                    mSelectedListener!!.onItemSelectEnd()
                }
                if (lockGesture) {
                    enableGesture = true
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        mAnimation?.start()
    }

    private val intervalDistance: Int
        private get() = if (isHorizontal) {
            Math.round(mDecoratedChildWidth * mIntervalRatio)
        } else {
            Math.round(mDecoratedChildHeight * mIntervalRatio)
        }

    private fun onSelectedCallBack() {
        mSelectPositionNature = Math.round((mOffsetAll / intervalDistance).toFloat())
        selectedPos = mSelectPositionNature
        selectedPos = if (selectedPos < 0) itemCount + selectedPos % itemCount else Math.abs(
            selectedPos % itemCount
        )
        if (mSelectedListener != null && selectedPos != mLastSelectPosition) {
            mSelectedListener!!.onItemSelected(selectedPos)
        }
        mLastSelectPosition = selectedPos
    }

    private fun checkTag(tag: Any?): TAG? {
        return if (tag != null) {
            if (tag is TAG) {
                tag
            } else {
                throw IllegalArgumentException("You should not use View#setTag(Object tag), use View#setTag(int key, Object tag) instead!")
            }
        } else {
            null
        }
    }

    val firstVisiblePosition: Int
        get() {
            val displayFrame = Rect(mOffsetAll, 0, mOffsetAll + horizontalSpace, verticalSpace)
            val cur = centerPosition
            var i = cur - 1
            while (true) {
                val rect = getFrame(i)
                if (rect.left <= displayFrame.left) {
                    return Math.abs(i) % itemCount
                }
                i--
            }
        }
    val lastVisiblePosition: Int
        get() {
            val displayFrame = Rect(mOffsetAll, 0, mOffsetAll + horizontalSpace, verticalSpace)
            val cur = centerPosition
            var i = cur + 1
            while (true) {
                val rect = getFrame(i)
                if (rect.right >= displayFrame.right) {
                    return Math.abs(i) % itemCount
                }
                i++
            }
        }

    fun getChildActualPos(index: Int): Int {
        val child = getChildAt(index)
        return if (child!!.tag != null) {
            val tag = checkTag(child.tag)
            tag!!.pos
        } else {
            getPosition(child)
        }
    }

    val maxVisibleCount: Int
        get() {
            val oneSide = (horizontalSpace - mStartX) / intervalDistance
            return oneSide * 2 + 1
        }
    val centerPosition: Int
        get() {
            var pos = mOffsetAll / intervalDistance
            val more = mOffsetAll % intervalDistance
            if (Math.abs(more) >= intervalDistance * 0.5f) {
                if (more >= 0) {
                    pos++
                } else {
                    pos--
                }
            }
            return pos
        }

    fun setOnSelectedListener(l: OnSelected?) {
        mSelectedListener = l
    }

    fun getmSelectPositionNature(): Int {
        return mSelectPositionNature
    }

    private var destoryed = false

    init {
        mIsFlatFlow = isFlat
        mItemGradualGrey = isGreyItem
        mItemGradualAlpha = isAlphaItem
        mIsLoop = isLoop
        this.orientation = orientation
        if (cstInterval >= 0) {
            mIntervalRatio = cstInterval
        } else {
            if (mIsFlatFlow) {
                mIntervalRatio = 1.1f
            }
        }
    }

    fun onDestory() {
        destoryed = true
        if (mAnimation != null) {
            mAnimation!!.cancel()
        }
    }

    interface OnSelected {
        fun onItemSelected(position: Int)
        fun onItemSelectStart() {}
        fun onItemSelectEnd() {}
    }

    private inner class TAG internal constructor(var pos: Int)
    internal class Builder {
        var isFlat = false
        var isGreyItem = false
        var isAlphaItem = false
        var cstIntervalRatio = -1f
        var isLoop = false
        var orientation = OrientationHelper.VERTICAL
        var is3DItem = false
        fun setFlat(flat: Boolean): Builder {
            isFlat = flat
            return this
        }

        fun setOrientation(orientation: Int): Builder {
            this.orientation = orientation
            return this
        }

        fun setGreyItem(greyItem: Boolean): Builder {
            isGreyItem = greyItem
            return this
        }

        fun setAlphaItem(alphaItem: Boolean): Builder {
            isAlphaItem = alphaItem
            return this
        }

        fun setIntervalRatio(ratio: Float): Builder {
            cstIntervalRatio = ratio
            return this
        }

        fun loop(): Builder {
            isLoop = true
            return this
        }

        fun set3DItem(d3: Boolean): Builder {
            is3DItem = d3
            return this
        }

        fun build(): CoverFlowLayoutManger {
            return CoverFlowLayoutManger(
                isFlat, isGreyItem,
                isAlphaItem, cstIntervalRatio, isLoop, is3DItem, orientation
            )
        }
    }

    companion object {
        private const val SCROLL_TO_END = 1
        private const val SCROLL_TO_START = 2
        private const val MAX_RECT_COUNT = 100
        private const val smoothScrollDuration: Long = 500
    }
}