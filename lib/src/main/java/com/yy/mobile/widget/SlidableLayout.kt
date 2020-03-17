package com.yy.mobile.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.IntDef
import android.support.v4.app.Fragment
import android.support.v4.view.NestedScrollingChild2
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.NestedScrollingParent2
import android.support.v4.view.NestedScrollingParentHelper
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewCompat.TYPE_NON_TOUCH
import android.support.v4.view.ViewCompat.TYPE_TOUCH
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.Scroller
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Layout supports Sliding.
 * Use the [setAdapter] to construct the view.
 *
 * The child view in the layout can implement the [SlidableUI] and listen to
 * the callback method.
 *
 * @see SlideViewAdapter adapt to the sliding of [View].
 * @see SlideFragmentAdapter adapt to the sliding of [Fragment].
 *
 * ——————————————————————————————————————————————————————————————————————————————
 * 支持上下滑的布局。
 * 使用 [setAdapter] 方法来构造上下滑切换的视图。
 *
 * 布局中的子视图可以实现[SlidableUI]方法，监听对应的回调方法。
 *
 * 可以直接对 [View] 进行上下滑，参考 [SlideAdapter] 或者 [SlideViewAdapter]。
 * 可以对 [Fragment] 进行上下滑，参考 [SlideFragmentAdapter]。
 *
 * @author YvesCheung
 * 2019/4/11
 */
@Suppress("MemberVisibilityCanBePrivate")
class SlidableLayout : FrameLayout, NestedScrollingChild2, NestedScrollingParent2 {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr) {
        initAttr(context, attrs, defStyleAttr)
    }

    companion object {

        const val HORIZONTAL = 0
        const val VERTICAL = 1

        private const val DEBUG = false

        private const val MIN_FLING_VELOCITY = 400 // dips

        const val MAX_DURATION = 600 //最大滑行时间ms

        private val sInterpolator = Interpolator { t ->
            val f = t - 1.0f
            f * f * f * f * f + 1.0f
        }
    }

    @IntDef(value = [HORIZONTAL, VERTICAL])
    annotation class OrientationMode

    @OrientationMode
    var orientation: Int = VERTICAL
        set(value) {
            if (value != HORIZONTAL && value != VERTICAL) {
                throw IllegalArgumentException(
                    "orientation should be 'SlidableLayout.HORIZONTAL' or 'SlidableLayout.VERTICAL'.")
            }
            if (mState != State.IDLE) {
                throw IllegalStateException(
                    "Can't change orientation when the layout is not IDLE.")
            }
            field = value
        }

    private val mMinFlingSpeed: Float //定义滑动速度足够快的标准

    private val childHelper = NestedScrollingChildHelper(this)
    private val parentHelper = NestedScrollingParentHelper(this)

    private val mTouchSlop: Int

    private val mDataObservable = SlidableDataObservable()
    private val mDataObserver = Observer()

    init {
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledPagingTouchSlop

        val density = context.resources.displayMetrics.density
        mMinFlingSpeed = MIN_FLING_VELOCITY * density

        isNestedScrollingEnabled = true
    }

    private fun initAttr(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.SlidableLayout, defStyleAttr, 0)
        orientation = a.getInt(R.styleable.SlidableLayout_android_orientation, VERTICAL)
        a.recycle()
    }

    @Suppress("unused")
    private enum class State(val flag: Int) {
        /**
         * IDLE
         * 静止状态
         */
        IDLE(Mask.IDLE),
        /**
         * Dragging to the next page
         * 正在向下一页拖动
         */
        SLIDE_NEXT(Mask.SLIDE or Mask.NEXT),
        /**
         * Dragging to the previous page
         * 正在向上一页拖动
         */
        SLIDE_PREV(Mask.SLIDE or Mask.PREV),
        /**
         * Can't drag to next page
         * 无法拖动到下一页
         */
        SLIDE_REJECT_NEXT(Mask.REJECT or Mask.SLIDE or Mask.NEXT),
        /**
         * Can't drag to previous page
         * 无法拖动到上一页
         */
        SLIDE_REJECT_PREV(Mask.REJECT or Mask.SLIDE or Mask.PREV),
        /**
         * Coasting to the next page
         * 手指离开，惯性滑行到下一页
         */
        FLING_NEXT(Mask.FLING or Mask.NEXT),
        /**
         * Coasting to the previous page
         * 手指离开，惯性滑行到上一页
         */
        FLING_PREV(Mask.FLING or Mask.PREV);

        infix fun satisfy(mask: Int): Boolean =
            flag and mask == mask

        companion object {

            fun of(vararg mask: Int): State {
                val flag = mask.fold(0) { acc, next -> acc or next }
                return values().first { it.flag == flag }
            }
        }
    }

    private object Mask {
        const val IDLE = 0b000001
        const val NEXT = 0b000010
        const val PREV = 0b000100
        const val SLIDE = 0b001000
        const val FLING = 0b010000
        const val REJECT = 0b100000
    }

    private var mState = State.of(Mask.IDLE)

    private val mInflater by lazy(NONE) { LayoutInflater.from(context) }

    private val mScroller = Scroller(context, sInterpolator)
    private var mAnimator: ValueAnimator? = null

    private var mViewHolderDelegate: ViewHolderDelegate<out SlideViewHolder>? = null

    private inline val mCurrentView: View?
        get() = mViewHolderDelegate?.currentViewHolder?.view

    private inline val mBackupView: View?
        get() = mViewHolderDelegate?.backupViewHolder?.view

    private var downY = 0f
    private var downX = 0f

    private var mScrollConsumed: IntArray = IntArray(2)
    private var mScrollOffset: IntArray = IntArray(2)

    private val mGestureCallback = GestureCallback()
    private val mHorizontalGesture = HorizontalMode()
    private val mVerticalGesture = VerticalMode()
    private inline val mGesture
        get() = if (orientation == HORIZONTAL) mHorizontalGesture else mVerticalGesture

    private inline val mScrollAxis
        get() = if (orientation == HORIZONTAL) ViewCompat.SCROLL_AXIS_HORIZONTAL else ViewCompat.SCROLL_AXIS_VERTICAL

    private val gestureDetector = GestureDetector(context, mGestureCallback)

    private var shouldDetermineIfStartNestedScroll = false
    private var nestedScrolling = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        if (gestureDetector.onTouchEvent(event)) {
            return true
        } else if (action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_CANCEL) {
            log("onUp $action state = $mState")
            if (mGestureCallback.onUp()) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return mGestureCallback.onInterceptTouchEvent(event) || super.onInterceptTouchEvent(event)
    }

    // just like ViewPager
    private fun calculateDuration(velocity: Float, maxDistance: Int, currentDistance: Int): Int {

        // We want the duration of the page snap animation to be influenced by the distance that
        // the screen has to travel, however, we don't want this duration to be effected in a
        // purely linear fashion. Instead, we use this method to moderate the effect that the distance
        // of travel has on the overall snap duration.
        fun distanceInfluenceForSnapDuration(f: Float): Float {
            var t: Double = f.toDouble()
            t -= 0.5 // center the values about 0.
            t *= 0.3 * Math.PI / 2.0
            return sin(t).toFloat()
        }

        val half = maxDistance / 2
        val distanceRatio = min(1f, abs(currentDistance).toFloat() / maxDistance)
        val distance = half + half * distanceInfluenceForSnapDuration(distanceRatio)

        val v = abs(velocity)
        val duration: Int =
            if (v > 0) {
                4 * (1000 * abs(distance / v)).roundToInt()
            } else {
                val pageDelta = abs(currentDistance).toFloat() / maxDistance
                ((pageDelta + 1f) * 100).toInt()
            }
        return min(duration, MAX_DURATION)
    }

    private fun requestParentDisallowInterceptTouchEvent() {
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun hasNestedScrollingParent() = childHelper.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int) = childHelper.hasNestedScrollingParent(type)

    override fun isNestedScrollingEnabled() = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int) = childHelper.startNestedScroll(axes)

    override fun startNestedScroll(axes: Int, type: Int) = childHelper.startNestedScroll(axes, type)

    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)

    override fun stopNestedScroll() = childHelper.stopNestedScroll()

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int
    ) = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
        dyUnconsumed, offsetInWindow, type)

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ) = childHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
        dxUnconsumed, dyUnconsumed, offsetInWindow)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?, type: Int
    ) = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?
    ) = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean) =
        childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float) =
        childHelper.dispatchNestedPreFling(velocityX, velocityY)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) =
        parentHelper.onNestedScrollAccepted(child, target, axes)

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) =
        parentHelper.onNestedScrollAccepted(child, target, axes, type)

    override fun getNestedScrollAxes(): Int = parentHelper.nestedScrollAxes

    override fun onStopNestedScroll(child: View) {
        log("onStopNestedScroll")
        val topView = mCurrentView
        val backView = mBackupView
        if (mState satisfy Mask.SLIDE && topView != null && backView != null) {
            mGestureCallback.performFling(topView, backView, 0f, 0f)
        } else if (!(mState satisfy Mask.FLING)) {
            mState = State.IDLE
        }
        nestedScrolling = false
        shouldDetermineIfStartNestedScroll = false
        stopNestedScroll()
        parentHelper.onStopNestedScroll(child)
    }

    override fun onStopNestedScroll(child: View, type: Int) = onStopNestedScroll(child)

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return onStartNestedScroll(child, target, nestedScrollAxes, TYPE_TOUCH)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, TYPE_TOUCH)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, TYPE_TOUCH)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        val matchHorizontalMode =
            orientation == HORIZONTAL && axes and ViewCompat.SCROLL_AXIS_HORIZONTAL != 0
        val matchVerticalMode =
            orientation == VERTICAL && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0

        log("onStartNestedScroll target = $target type = $type " +
            "matchHorizontal = $matchHorizontalMode " +
            "matchVertical = $matchVerticalMode")
        if (type == TYPE_TOUCH && (matchHorizontalMode || matchVerticalMode)) {
            shouldDetermineIfStartNestedScroll = true
            nestedScrolling = true
            startNestedScroll(axes)
            return true
        }
        return false
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        val topView = mCurrentView
        val backView = mBackupView
        if (!(mState satisfy Mask.REJECT) && mState satisfy Mask.SLIDE && topView != null && backView != null) {
            val dxFromDownX = topView.x - dx
            val dyFromDownY = topView.y - dy
            mGesture.scrollChildView(topView, backView,
                dxFromDownX, dyFromDownY, dx, dy)
            consumed[0] = dx
            consumed[1] = dy
            return
        }
        dispatchNestedPreScroll(dx, dy, consumed, null, type)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {

        fun dispatchToChild() {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type)
        }

        val delegate = mViewHolderDelegate
        val topView = mCurrentView
        if (delegate == null || topView == null ||
            (dxUnconsumed == 0 && dyUnconsumed == 0)) {
            return dispatchToChild()
        }

        val adapter = delegate.adapter

        val dxFromDownX = topView.x - dxUnconsumed
        val dyFromDownY = topView.y - dyUnconsumed

        if (shouldDetermineIfStartNestedScroll) {
            shouldDetermineIfStartNestedScroll = false
            val direction = mGesture.gestureDirection(dxFromDownX, dyFromDownY)
            val startToMove = mGesture.isStartToMove(dxFromDownX, dyFromDownY)
            val changeDirection = mGesture.isChangeDirection(dxFromDownX, dyFromDownY)

            if (startToMove || changeDirection) {
                val directionMask =
                    if (direction == SlideDirection.Next) Mask.NEXT else Mask.PREV
                if (!adapter.canSlideTo(direction)) {
                    mState = State.of(directionMask, Mask.SLIDE, Mask.REJECT)
                } else {
                    mState = State.of(directionMask, Mask.SLIDE)
                    delegate.prepareBackup(direction)
                }
            }
            log("onNestedScroll dx = $dxFromDownX dy = $dyFromDownY type = $type " +
                "startToMove = $startToMove changeDirection = $changeDirection state = $mState")
        } else {
            log("onNestedScroll dx = $dxFromDownX dy = $dyFromDownY type = $type state = $mState")
        }

        val backView = mBackupView
        if (!(mState satisfy Mask.REJECT) && mState satisfy Mask.SLIDE && backView != null) {
            mGesture.scrollChildView(topView, backView,
                dxFromDownX, dyFromDownY,
                dxUnconsumed, dyUnconsumed)
            return
        }

        dispatchToChild()
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        log("onNestedFling vx = $velocityX vy = $velocityY consumed = $consumed")
        if (!consumed) {
            val topView = mCurrentView
            val backView = mBackupView
            if (topView != null && backView != null &&
                mGestureCallback.performFling(topView, backView, velocityX, velocityY)) {
                return true
            }
        }
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    @Suppress("ConstantConditionIf")
    private fun log(str: String) {
        if (DEBUG) Log.i("SlidableLayout", str)
    }

    /**
     * Set a new adapter to provide child views.
     *
     * @see SlideViewAdapter
     * @see SlideFragmentAdapter
     */
    fun setAdapter(adapter: SlideAdapter<out SlideViewHolder>?) {
        if (mViewHolderDelegate != null) {
            removeAllViews()
            unregisterDataSetObserver(mDataObserver)
        }
        if (adapter != null) {
            mViewHolderDelegate =
                ViewHolderDelegate(adapter).apply {
                    prepareCurrent(SlideDirection.Origin)
                    onCompleteCurrent(SlideDirection.Origin, true)
                }
            registerDataSetObserver(mDataObserver)
        }
    }

    /**
     * Automatically slide the view in the [direction] direction.
     * This method will work when and only when the current state is [State.IDLE].
     *
     * ————————————————————————————————————————————————————————————————————————————————————————
     * 自动滑到 [direction] 方向的视图。
     * 当且仅当布局处于静止状态时有效。
     *
     * @param direction the slide direction：[SlideDirection.Next] or [SlideDirection.Prev]
     *
     * @return true if successfully sliding.
     */
    fun slideTo(direction: SlideDirection): Boolean {
        if (direction != SlideDirection.Origin &&
            mState satisfy Mask.IDLE) {

            val delegate = mViewHolderDelegate
                ?: return false
            val adapter = delegate.adapter

            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_NON_TOUCH)
            requestParentDisallowInterceptTouchEvent()

            //Simulate sliding at a [mockSpeed] in this direction
            val directionMask =
                if (direction == SlideDirection.Prev) Mask.PREV else Mask.NEXT
            val mockSpeed =
                if (direction == SlideDirection.Prev) mMinFlingSpeed else -mMinFlingSpeed

            mState =
                if (adapter.canSlideTo(direction)) {
                    delegate.prepareBackup(direction)
                    State.of(directionMask, Mask.SLIDE)
                } else {
                    State.of(directionMask, Mask.SLIDE, Mask.REJECT)
                }

            val canSlide = !(mState satisfy Mask.REJECT)
            log("Auto slide to $direction" + if (canSlide) "" else " but reject")
            if (orientation == VERTICAL) {
                mGestureCallback.onUp(0f, mockSpeed)
            } else {
                mGestureCallback.onUp(mockSpeed, 0f)
            }
            return canSlide
        }
        return false
    }

    @Deprecated(
        message = "Use slideTo(direction) instead.",
        replaceWith = ReplaceWith("slideTo(direction)"))
    fun slideTo(direction: SlideDirection, duration: Int) =
        slideTo(direction)

    /**
     * Register a new observer to listen for data changes.
     *
     * @see unregisterDataSetObserver
     */
    fun registerDataSetObserver(observer: SlidableDataObserver) {
        mDataObservable.registerObserver(observer)
    }

    /**
     * Unregister an observer currently listening for data changes.
     *
     * @see registerDataSetObserver
     */
    fun unregisterDataSetObserver(observer: SlidableDataObserver) {
        mDataObservable.unregisterObserver(observer)
    }

    /**
     * Notify any registered observers that the data set has changed.
     */
    fun notifyDataSetChanged() {
        mDataObservable.notifyDataSetChanged()
    }

    private inner class ViewHolderDelegate<ViewHolder : SlideViewHolder>(
        val adapter: SlideAdapter<ViewHolder>
    ) {

        var currentViewHolder: ViewHolder? = null

        var backupViewHolder: ViewHolder? = null

        private fun ViewHolder?.prepare(direction: SlideDirection): ViewHolder {
            val holder = this ?: adapter.onCreateViewHolder(context, this@SlidableLayout, mInflater)
            if (holder.view.parent == null) {
                addView(holder.view, 0)
            }
            adapter.onBindView(holder, direction)
            return holder
        }

        fun prepareCurrent(direction: SlideDirection) =
            currentViewHolder.prepare(direction).also { currentViewHolder = it }

        fun prepareBackup(direction: SlideDirection) =
            backupViewHolder.prepare(direction).also { backupViewHolder = it }

        fun onCompleteCurrent(direction: SlideDirection, isInit: Boolean = false) {
            currentViewHolder?.let {
                if (isInit) {
                    it.view.post {
                        adapter.onViewComplete(it, direction)
                    }
                } else {
                    adapter.onViewComplete(it, direction)
                }
            }
        }

        fun finishSlide(direction: SlideDirection) {
            val visible = currentViewHolder
            val dismiss = backupViewHolder
            if (visible != null && dismiss != null) {
                adapter.finishSlide(dismiss, visible, direction)
            }
        }

        fun onDismissBackup(direction: SlideDirection) {
            backupViewHolder?.let { adapter.onViewDismiss(it, this@SlidableLayout, direction) }
        }

        fun swap() {
            val tmp = currentViewHolder
            currentViewHolder = backupViewHolder
            backupViewHolder = tmp
        }
    }

    private inner class Observer : SlidableDataObserver {

        override fun onChanged() {
            assertNotInLayoutOrScroll()
            mViewHolderDelegate?.apply {
                prepareCurrent(SlideDirection.Origin)
                onCompleteCurrent(SlideDirection.Origin, true)
            }
        }

        private fun assertNotInLayoutOrScroll() {
            if (mState satisfy Mask.IDLE || mState satisfy Mask.REJECT) {
                return
            }
            throw IllegalStateException("Cannot call this method while RecyclerView is "
                + "computing a layout or scrolling")
        }
    }

    private inner class GestureCallback : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            if (mState satisfy Mask.FLING) {
                mGesture.dontConsumeTouchEvent(distanceX, distanceY)
                return true
            }
            val topView = mCurrentView ?: return false
            val delegate = mViewHolderDelegate
                ?: return false
            val adapter = delegate.adapter

            var dyFromDownY = e2.y - downY
            var dxFromDownX = e2.x - downX

            val direction = mGesture.gestureDirection(dxFromDownX, dyFromDownY)

            val startToMove = mGesture.isStartToMove(dxFromDownX, dyFromDownY)

            val changeDirection = mGesture.isChangeDirection(dxFromDownX, dyFromDownY)

            var dx = distanceX.toInt()
            var dy = distanceY.toInt()
            if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                dx -= mScrollConsumed[0]
                dy -= mScrollConsumed[1]
                dxFromDownX -= mScrollConsumed[0]
                dyFromDownY -= mScrollConsumed[1]
            }

            if (startToMove) {
                requestParentDisallowInterceptTouchEvent()
            }

            if (startToMove || changeDirection) {
                val directionMask =
                    if (direction == SlideDirection.Next) Mask.NEXT else Mask.PREV

                if (!adapter.canSlideTo(direction)) {
                    mState = State.of(directionMask, Mask.SLIDE, Mask.REJECT)
                } else {
                    mState = State.of(directionMask, Mask.SLIDE)
                    delegate.prepareBackup(direction)
                }
                log("onMove state = $mState, start = $startToMove, " +
                    "changeDirection = $changeDirection")
            }
            if (mState satisfy Mask.REJECT || mState satisfy Mask.IDLE) {
                return dispatchNestedScroll(mScrollConsumed[0], mScrollConsumed[1], dx, dy, mScrollOffset)

            } else if (mState satisfy Mask.SLIDE) {
                val backView = mBackupView ?: return false
                return mGesture.scrollChildView(topView, backView, dxFromDownX, dyFromDownY, dx, dy)
            }
            return false
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            log("onFling ${e2.action} vY = $velocityY state = $mState")
            onUp(velocityX, velocityY)
            return true
        }

        fun onUp(velocityX: Float = 0f, velocityY: Float = 0f): Boolean {
            if (!(mState satisfy Mask.SLIDE)) {
                stopNestedScroll()
                return false
            }

            val topView = mCurrentView ?: return resetTouch()
            val currentOffsetY = topView.y.toInt()
            val currentOffsetX = topView.x.toInt()
            // if state is reject, don't consume the flingChildView.
            val consumedFling = mGesture.shouldConsumedFling(currentOffsetX, currentOffsetY)
            if (!dispatchNestedPreFling(velocityX, velocityY)) {
                dispatchNestedFling(velocityX, velocityY, consumedFling)
            }
            stopNestedScroll()

            val backView = mBackupView
            if (backView != null &&
                performFling(topView, backView, velocityX, velocityY)) {
                return true
            }

            return resetTouch()
        }

        fun performFling(
            topView: View, backView: View,
            velocityX: Float, velocityY: Float
        ): Boolean {
            val currentOffsetX = topView.x.toInt()
            val currentOffsetY = topView.y.toInt()

            val delegate = mViewHolderDelegate

            var direction: SlideDirection? = null
            var duration: Int? = null

            val widgetHeight = measuredHeight
            val widgetWidth = measuredWidth
            if (mGesture.shouldConsumedFling(currentOffsetX, currentOffsetY)) {
                var dy: Int? = null
                var dx: Int? = null

                if (mGesture.isFling(currentOffsetX, currentOffsetY, velocityX, velocityY)) {
                    if (mState == State.SLIDE_NEXT) {
                        direction = SlideDirection.Next
                        dy = -currentOffsetY - widgetHeight
                        dx = -currentOffsetX - widgetWidth
                    } else if (mState == State.SLIDE_PREV) {
                        direction = SlideDirection.Prev
                        dy = widgetHeight - currentOffsetY
                        dx = widgetWidth - currentOffsetX
                    }
                } else { //back to origin
                    direction = SlideDirection.Origin
                    dy = -currentOffsetY
                    dx = -currentOffsetX
                }

                duration = mGesture.startScroll(currentOffsetX, currentOffsetY,
                    dx, dy, velocityX, velocityY)
            }

            //perform flingChildView animation
            if (delegate != null && direction != null && duration != null) {
                mAnimator?.cancel()
                mAnimator = ValueAnimator.ofFloat(1f).apply {
                    setDuration(duration.toLong())
                    addUpdateListener {
                        if (mScroller.computeScrollOffset()) {
                            mGesture.flingChildView(topView, backView, currentOffsetX, currentOffsetY)
                        }
                    }
                    addListener(object : AnimatorListenerAdapter() {

                        override fun onAnimationCancel(animation: Animator?) =
                            onAnimationEnd(animation)

                        override fun onAnimationEnd(animation: Animator?) {
                            if (direction != SlideDirection.Origin) {
                                delegate.swap()
                            }
                            delegate.onDismissBackup(direction)
                            mState = State.of(Mask.IDLE)
                            if (direction != SlideDirection.Origin) {
                                delegate.onCompleteCurrent(direction)
                            }
                            delegate.finishSlide(direction)
                        }
                    })
                    start()
                }

                val directionMask = if (mState satisfy Mask.NEXT) Mask.NEXT else Mask.PREV
                mState = State.of(directionMask, Mask.FLING)
                return true
            }
            return false
        }

        private fun resetTouch(): Boolean {
            mState = State.of(Mask.IDLE)
            mBackupView?.let(::removeView)
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            downY = e.y
            downX = e.x
            startNestedScroll(mScrollAxis)
            return true
        }

        fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            val action = event.action and MotionEvent.ACTION_MASK
            log("onInterceptTouchEvent action = $action, state = $mState")
            var intercept = false

            if (action != MotionEvent.ACTION_MOVE) {
                if (mState != State.IDLE) {
                    intercept = true
                }
            }

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    startNestedScroll(mScrollAxis)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = abs(event.y - downY)
                    val dx = abs(event.x - downX)
                    if (!nestedScrolling && mGesture.interceptTouchEvent(dx, dy)) {
                        log("onInterceptTouchEvent requestDisallow")
                        requestParentDisallowInterceptTouchEvent()
                        intercept = true
                    }
                }
            }
            return intercept
        }
    }

    private interface OrientationGestureCallback {

        fun interceptTouchEvent(dxFromDownX: Float, dyFromDownY: Float): Boolean

        fun gestureDirection(dxFromDownX: Float, dyFromDownY: Float): SlideDirection

        fun isStartToMove(dxFromDownX: Float, dyFromDownY: Float): Boolean

        fun isChangeDirection(dxFromDownX: Float, dyFromDownY: Float): Boolean

        fun scrollChildView(
            topView: View,
            backView: View,
            dxFromDownX: Float,
            dyFromDownY: Float,
            dx: Int,
            dy: Int
        ): Boolean

        fun shouldConsumedFling(offsetX: Int, offsetY: Int): Boolean

        fun isFling(offsetX: Int, offsetY: Int, velocityX: Float, velocityY: Float): Boolean

        fun startScroll(
            offsetX: Int,
            offsetY: Int,
            dx: Int?,
            dy: Int?,
            velocityX: Float,
            velocityY: Float
        ): Int?

        fun flingChildView(topView: View, backView: View, offsetX: Int, offsetY: Int)

        fun dontConsumeTouchEvent(dx: Float, dy: Float)
    }

    private inner class HorizontalMode : OrientationGestureCallback {

        override fun interceptTouchEvent(dxFromDownX: Float, dyFromDownY: Float): Boolean {
            return dxFromDownX > mTouchSlop && dxFromDownX > 2 * dyFromDownY
        }

        override fun gestureDirection(dxFromDownX: Float, dyFromDownY: Float): SlideDirection =
            when {
                dxFromDownX < 0 -> SlideDirection.Next
                dxFromDownX > 0 -> SlideDirection.Prev
                else -> SlideDirection.Origin
            }

        override fun isStartToMove(dxFromDownX: Float, dyFromDownY: Float): Boolean {
            return mState satisfy Mask.IDLE && abs(dxFromDownX) > 2 * abs(dyFromDownY)
        }

        override fun isChangeDirection(dxFromDownX: Float, dyFromDownY: Float): Boolean {
            return (mState satisfy Mask.PREV && dxFromDownX < 0) ||
                (mState satisfy Mask.NEXT && dxFromDownX > 0)
        }

        override fun scrollChildView(
            topView: View,
            backView: View,
            dxFromDownX: Float,
            dyFromDownY: Float,
            dx: Int,
            dy: Int
        ): Boolean {
            topView.x = dxFromDownX
            backView.x =
                if (mState satisfy Mask.NEXT) dxFromDownX + measuredWidth
                else dxFromDownX - measuredWidth
            return dispatchNestedScroll(dx, 0, 0, dy, mScrollOffset)
        }

        override fun shouldConsumedFling(offsetX: Int, offsetY: Int): Boolean {
            return !(mState satisfy Mask.REJECT) || offsetX != 0
        }

        override fun isFling(offsetX: Int, offsetY: Int, velocityX: Float, velocityY: Float): Boolean {
            val highSpeed = abs(velocityX) >= mMinFlingSpeed

            val sameDirection =
                (mState == State.SLIDE_NEXT && velocityX < 0) ||
                    (mState == State.SLIDE_PREV && velocityX > 0)

            val moveLongDistance = abs(offsetX) > measuredWidth / 3

            return (highSpeed && sameDirection) || (!highSpeed && moveLongDistance)
        }

        override fun startScroll(
            offsetX: Int,
            offsetY: Int,
            dx: Int?,
            dy: Int?,
            velocityX: Float,
            velocityY: Float
        ): Int? {
            if (dx != null) {
                val duration = calculateDuration(velocityX, measuredWidth, dx)
                mScroller.startScroll(offsetX, 0, dx, 0, duration)
                return duration
            }
            return null
        }

        override fun flingChildView(topView: View, backView: View, offsetX: Int, offsetY: Int) {
            val widgetWidth = measuredWidth
            val offset = mScroller.currX.toFloat()
            topView.x = offset
            backView.x =
                if (mState == State.FLING_NEXT) offset + widgetWidth
                else offset - widgetWidth
        }

        override fun dontConsumeTouchEvent(dx: Float, dy: Float) {
            //eat all the dx
            val consumedX = dx.toInt()
            val unconsumedY = dy.toInt()
            if (!dispatchNestedPreScroll(consumedX, unconsumedY, mScrollConsumed,
                    mScrollOffset, TYPE_NON_TOUCH)) {
                dispatchNestedScroll(consumedX, 0, 0, unconsumedY,
                    mScrollOffset, TYPE_NON_TOUCH)
            }
        }
    }

    private inner class VerticalMode : OrientationGestureCallback {

        override fun interceptTouchEvent(dxFromDownX: Float, dyFromDownY: Float): Boolean {
            return dyFromDownY > mTouchSlop && dyFromDownY > 2 * dxFromDownX
        }

        override fun gestureDirection(dxFromDownX: Float, dyFromDownY: Float): SlideDirection =
            when {
                dyFromDownY < 0 -> SlideDirection.Next
                dyFromDownY > 0 -> SlideDirection.Prev
                else -> SlideDirection.Origin
            }

        override fun isStartToMove(dxFromDownX: Float, dyFromDownY: Float): Boolean {
            return mState satisfy Mask.IDLE && abs(dyFromDownY) > 2 * abs(dxFromDownX)
        }

        override fun isChangeDirection(dxFromDownX: Float, dyFromDownY: Float): Boolean {
            return (mState satisfy Mask.PREV && dyFromDownY < 0) ||
                (mState satisfy Mask.NEXT && dyFromDownY > 0)
        }

        override fun scrollChildView(
            topView: View,
            backView: View,
            dxFromDownX: Float,
            dyFromDownY: Float,
            dx: Int,
            dy: Int
        ): Boolean {
            topView.y = dyFromDownY
            backView.y =
                if (mState satisfy Mask.NEXT) dyFromDownY + measuredHeight
                else dyFromDownY - measuredHeight
            return dispatchNestedScroll(0, dy, dx, 0, mScrollOffset)
        }

        override fun shouldConsumedFling(offsetX: Int, offsetY: Int): Boolean {
            return !(mState satisfy Mask.REJECT) || offsetY != 0
        }

        override fun isFling(offsetX: Int, offsetY: Int, velocityX: Float, velocityY: Float): Boolean {
            val highSpeed = abs(velocityY) >= mMinFlingSpeed

            val sameDirection =
                (mState == State.SLIDE_NEXT && velocityY < 0) ||
                    (mState == State.SLIDE_PREV && velocityY > 0)

            val moveLongDistance = abs(offsetY) > measuredHeight / 3

            return (highSpeed && sameDirection) || (!highSpeed && moveLongDistance)
        }

        override fun startScroll(
            offsetX: Int,
            offsetY: Int,
            dx: Int?,
            dy: Int?,
            velocityX: Float,
            velocityY: Float
        ): Int? {
            if (dy != null) {
                val duration = calculateDuration(velocityY, measuredHeight, dy)
                mScroller.startScroll(0, offsetY, 0, dy, duration)
                return duration
            }
            return null
        }

        override fun flingChildView(topView: View, backView: View, offsetX: Int, offsetY: Int) {
            val widgetHeight = measuredHeight
            val offset = mScroller.currY.toFloat()
            topView.y = offset
            backView.y =
                if (mState == State.FLING_NEXT) offset + widgetHeight
                else offset - widgetHeight
        }

        override fun dontConsumeTouchEvent(dx: Float, dy: Float) {
            //eat all the dy
            val unconsumedX = dx.toInt()
            val consumedY = dy.toInt()
            if (!dispatchNestedPreScroll(unconsumedX, consumedY, mScrollConsumed,
                    mScrollOffset, TYPE_NON_TOUCH)) {
                dispatchNestedScroll(0, consumedY, unconsumedX, 0,
                    mScrollOffset, TYPE_NON_TOUCH)
            }
        }
    }
}