package com.cards.coverflow

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.cards.coverflow.CoverFlowLayoutManger.OnSelected
import android.view.MotionEvent

class RecyclerCoverFlow : RecyclerView {
    private var mDownX = 0f
    private var mDownY = 0f
    private var mManagerBuilder: CoverFlowLayoutManger.Builder? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) {
        init()
    }

    private fun init() {
        createManageBuilder()
        layoutManager = mManagerBuilder!!.build()
        isChildrenDrawingOrderEnabled = true
        overScrollMode = OVER_SCROLL_NEVER
    }

    private fun createManageBuilder() {
        if (mManagerBuilder == null) {
            mManagerBuilder = CoverFlowLayoutManger.Builder()
        }
    }

    fun setFlatFlow(isFlat: Boolean) {
        createManageBuilder()
        mManagerBuilder!!.setFlat(isFlat)
        layoutManager = mManagerBuilder!!.build()
    }

    fun setGreyItem(greyItem: Boolean) {
        createManageBuilder()
        mManagerBuilder!!.setGreyItem(greyItem)
        layoutManager = mManagerBuilder!!.build()
    }

    fun setAlphaItem(alphaItem: Boolean) {
        createManageBuilder()
        mManagerBuilder!!.setAlphaItem(alphaItem)
        layoutManager = mManagerBuilder!!.build()
    }

    fun setLoop() {
        createManageBuilder()
        mManagerBuilder!!.loop()
        layoutManager = mManagerBuilder!!.build()
    }

    fun set3DItem(d3: Boolean) {
        createManageBuilder()
        mManagerBuilder!!.set3DItem(d3)
        layoutManager = mManagerBuilder!!.build()
    }

    fun setIntervalRatio(intervalRatio: Float) {
        createManageBuilder()
        mManagerBuilder!!.setIntervalRatio(intervalRatio)
        layoutManager = mManagerBuilder!!.build()
    }

    fun setOrientation(orientation: Int) {
        createManageBuilder()
        mManagerBuilder!!.setOrientation(orientation)
        layoutManager = mManagerBuilder!!.build()
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        require(layout is CoverFlowLayoutManger) { "The layout manager must be CoverFlowLayoutManger" }
        super.setLayoutManager(layout)
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        val center = coverFlowLayout!!.centerPosition
        // 获取 RecyclerView 中第 i 个 子 view 的实际位置
        val actualPos = coverFlowLayout!!.getChildActualPos(i)

        // 距离中间item的间隔数
        val dist = actualPos - center
        var order: Int
        order = if (dist < 0) { // [< 0] 说明 item 位于中间 item 左边，按循序绘制即可
            i
        } else { // [>= 0] 说明 item 位于中间 item 右边，需要将顺序颠倒绘制
            childCount - 1 - dist
        }
        if (order < 0) {
            order = 0
        } else if (order > childCount - 1) {
            order = childCount - 1
        }
        return order
    }

    val coverFlowLayout: CoverFlowLayoutManger?
        get() = layoutManager as CoverFlowLayoutManger?
    val selectedPos: Int
        get() = coverFlowLayout!!.selectedPos
    val selectedNaturePos: Int
        get() = coverFlowLayout!!.getmSelectPositionNature()

    fun randomSmoothScrollToPosition(): Boolean {
        return coverFlowLayout!!.randomSmoothScrollToPosition()
    }

    fun randomSmoothScrollToPosition(duration: Long): Boolean {
        return coverFlowLayout!!.randomSmoothScrollToPosition(duration)
    }

    fun randomSmoothScrollToPosition(duration: Long, pos: Int): Boolean {
        return coverFlowLayout!!.randomSmoothScrollToPosition(duration, pos)
    }

    fun setOnItemSelectedListener(l: OnSelected?) {
        coverFlowLayout!!.setOnSelectedListener(l)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> if (coverFlowLayout!!.isHorizontal) {
                mDownX = ev.x
                parent.requestDisallowInterceptTouchEvent(true)
            } else {
                mDownY = ev.y
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> if (coverFlowLayout!!.isHorizontal) {
                if (ev.x > mDownX && coverFlowLayout!!.centerPosition == 0 ||
                    ev.x < mDownX && coverFlowLayout!!.centerPosition ==
                    coverFlowLayout!!.itemCount - 1
                ) {
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            } else {
                if (ev.y > mDownY && coverFlowLayout!!.centerPosition == 0 ||
                    ev.y < mDownY && coverFlowLayout!!.centerPosition ==
                    coverFlowLayout!!.itemCount - 1
                ) {
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            else -> {}
        }
        return super.dispatchTouchEvent(ev)
    }
}