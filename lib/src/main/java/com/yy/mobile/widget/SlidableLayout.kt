package com.yy.mobile.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Scroller

/**
 * Created by 张宇 on 2019/4/11.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 *
 * 支持上下滑的布局。
 * 使用 [setAdapter] 方法来构造上下滑切换的视图。
 *
 * 可以直接对 [View] 进行上下滑，参考 [SlideAdapter] 或者 [SlideViewAdapter]。
 * 可以对 [Fragment] 进行上下滑，参考 [SlideFragmentAdapter]。
 *
 */
open class SlidableLayout : FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private enum class STATE {
        /**
         * 静止状态
         */
        IDLE,
        /**
         * 正在向下一页拖动
         */
        SLIDE_NEXT,
        /**
         * 正在向上一页拖动
         */
        SLIDE_PREV,
        /**
         * 无法拖动
         */
        SLIDE_REJECT,
        /**
         * 手指离开，惯性滑行到下一页
         */
        FLING_NEXT,
        /**
         * 手指离开，惯性滑行到上一页
         */
        FLING_PREV
    }

    private var mState = STATE.IDLE

    private val mInflater by lazy { LayoutInflater.from(context) }

    private val mScroller = Scroller(context)

    private var mViewHolderDelegate: ViewHolderDelegate<out SlideViewHolder>? = null

    private val mCurrentView: View?
        get() = mViewHolderDelegate?.currentViewHolder?.view

    private val mBackupView: View?
        get() = mViewHolderDelegate?.backupViewHolder?.view

    private val mGestureCallback = object : GestureDetector.SimpleOnGestureListener() {

        private var downY = 0f

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            if (mState != STATE.SLIDE_NEXT &&
                mState != STATE.SLIDE_PREV &&
                mState != STATE.IDLE) {
                return false
            }
            val topView = mCurrentView ?: return false
            val delegate = mViewHolderDelegate ?: return false
            val adapter = delegate.adapter

            val totalDistance = e2.y - downY
            val direction = when {
                distanceY > 0 -> SlideDirection.Next
                distanceY < 0 -> SlideDirection.Prev
                else -> SlideDirection.Origin
            }
            val startToMove = mState == STATE.IDLE && Math.abs(distanceY) > Math.abs(distanceX)
            val changeDirectionToNext = mState == STATE.SLIDE_PREV && totalDistance < 0
            val changeDirectionToPrev = mState == STATE.SLIDE_NEXT && totalDistance > 0

            if (startToMove || changeDirectionToNext || changeDirectionToPrev) {
                val action = adapter.canSlideTo(direction)
                mState =
                    if (action == SlideAction.Freeze) {
                        STATE.SLIDE_REJECT
                        return false
                    } else if (direction == SlideDirection.Next) {
                        STATE.SLIDE_NEXT
                    } else {
                        STATE.SLIDE_PREV
                    }
                delegate.prepareBackup(direction)
            }
            val backView = mBackupView ?: return false
            if (mState == STATE.SLIDE_NEXT || mState == STATE.SLIDE_PREV) {
                topView.y = totalDistance
                backView.y =
                    if (mState == STATE.SLIDE_NEXT) totalDistance + measuredHeight
                    else totalDistance - measuredHeight
                return true
            }
            return false
        }

        override fun onFling(
            e1: MotionEvent, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            return onUp(e2, velocityY)
        }

        fun onUp(e: MotionEvent, velocityY: Float = 0f): Boolean {
            if (mState != STATE.SLIDE_NEXT && mState != STATE.SLIDE_PREV) {
                return false
            }
            val backView = mBackupView ?: return false
            val topView = mCurrentView ?: return false
            val delegate = mViewHolderDelegate ?: return false
            val currentOffsetY = topView.y.toInt()
            var direction: SlideDirection? = null
            val duration = 250

            val highSpeed = Math.abs(velocityY) > 1000
            val sameDirection = (mState == STATE.SLIDE_NEXT && velocityY < 0) ||
                (mState == STATE.SLIDE_PREV && velocityY > 0)
            val moveLongDistance = Math.abs(currentOffsetY) > measuredHeight / 3
            if ((highSpeed && sameDirection) || (!highSpeed && moveLongDistance)) { //fling
                if (mState == STATE.SLIDE_NEXT) {
                    direction = SlideDirection.Next
                    mScroller.startScroll(0, currentOffsetY, 0,
                        -currentOffsetY - measuredHeight, duration)
                } else if (mState == STATE.SLIDE_PREV) {
                    direction = SlideDirection.Prev
                    mScroller.startScroll(0, currentOffsetY, 0,
                        measuredHeight - currentOffsetY, duration)
                }
            } else { //back to origin
                direction = SlideDirection.Origin
                mScroller.startScroll(0, currentOffsetY, 0, -currentOffsetY, duration)
            }

            if (direction != null) {
                val animator = ValueAnimator.ofFloat(1f)
                    .setDuration(duration.toLong())
                animator.addUpdateListener {
                    if (mScroller.computeScrollOffset()) {
                        val offset = mScroller.currY.toFloat()
                        topView.y = offset
                        backView.y =
                            if (mState == STATE.FLING_NEXT) offset + measuredHeight
                            else offset - measuredHeight
                    }
                }
                animator.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationCancel(animation: Animator?) =
                        onAnimationEnd(animation)

                    override fun onAnimationEnd(animation: Animator?) {
                        if (direction != SlideDirection.Origin) {
                            delegate.swap()
                        }
                        delegate.onDismissBackup(direction)
                        mState = STATE.IDLE
                        if (direction != SlideDirection.Origin) {
                            delegate.onCompleteCurrent(direction)
                        }
                        delegate.finishSlide(direction)
                    }
                })
                animator.start()
                mState = if (mState == STATE.SLIDE_NEXT) STATE.FLING_NEXT else STATE.FLING_PREV
                return true
            } else {
                mBackupView?.let(::removeView)
                mState = STATE.IDLE
                return false
            }
        }

        override fun onDown(e: MotionEvent): Boolean {
            downY = e.y
            return true
        }
    }
    private val gestureDetector = GestureDetector(context, mGestureCallback)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        } else if (event.action == MotionEvent.ACTION_UP
            || event.action == MotionEvent.ACTION_CANCEL) {
            if (mGestureCallback.onUp(event)) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setAdapter(adapter: SlideAdapter<out SlideViewHolder>) {
        removeAllViews()
        mViewHolderDelegate = ViewHolderDelegate(adapter).apply {
            prepareCurrent(SlideDirection.Origin)
            onCompleteCurrent(SlideDirection.Origin, true)
        }
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
}

/**
 * 适配 [SlidableLayout] 以及布局中滑动的 [View] 。
 *
 * 假如首次初始化页面【A】，触发的回调是：
 * - onCreateViewHolder(context, inflater)
 * - onViewComplete(viewHolder【A】)
 *
 * 假如从页面【A】滑动下一个页面【B】，触发的回调将会是：
 *
 * - canSlideTo(SlideDirection.Next)
 * - onCreateViewHolder(context, inflater) (如果是首次滑动)
 * - onBindView(viewHolder【B】, SlideDirection.Next)
 * - onViewDismiss(viewHolder【A】, SlideDirection.Next)
 * - onViewComplete(viewHolder【B】)
 * - finishSlide(SlideDirection.Next)
 *
 * 假如再从页面【B】 滑动回上一个页面 【A】，触发的回调是：
 *
 * - canSlideTo(SlideDirection.Prev)
 * - onBindView(viewHolder【A】, SlideDirection.Prev)
 * - onViewDismiss(viewHolder【B】, SlideDirection.Prev)
 * - onViewComplete(viewHolder【A】)
 * - finishSlide(SlideDirection.Prev)
 *
 * 假如从页面【A】试图滑动到页面【B】，但距离或者速度不够，所以放手后回弹到【A】，触发的回调是：
 *
 * - canSlideTo(SlideDirection.Next)
 * - onBindView(viewHolder【B】, SlideDirection.Next)
 * - onViewDismiss(viewHolder【B】, SlideDirection.Next)
 * - finishSlide(SlideDirection.Origin)
 */
interface SlideAdapter<ViewHolder : SlideViewHolder> {

    /**
     * 能否向 [direction] 的方向滑动。
     *
     * @param direction 滑动的方向
     *
     * @return 返回 [SlideAction.Slide] 表示可以开始滑动，
     * 返回 [SlideAction.Load] 表示可以滑动但显示加载中的动画，
     * 返回 [SlideAction.Freeze]  表示无法滑动
     */
    fun canSlideTo(direction: SlideDirection): SlideAction

    /**
     * 创建持有 [View] 的 [SlideViewHolder] 。
     * 一般来说，该方法会在 [SlidableLayout.setAdapter] 方法调用时触发一次，创建当前显示的 [View]，
     * 会在首次开始滑动时触发第二次，创建滑动目标的 [View]。
     */
    fun onCreateViewHolder(context: Context, parent: ViewGroup, inflater: LayoutInflater): ViewHolder

    /**
     * 当 [View] 开始滑动到可见时触发，在这个方法中实现数据和 [View] 的绑定。
     *
     * @param viewHolder 持有 [View] 的 [SlideViewHolder]
     * @param direction  滑动的方向
     */
    fun onBindView(viewHolder: ViewHolder, direction: SlideDirection)

    /**
     * 当 [View] 完全出现时触发。
     * 这个时机可能是 [SlidableLayout.setAdapter] 后 [View] 的第一次初始化，
     * 也可能是完成一次滑动，在 [finishSlide] 后 **而且** 滑到了一个新的 [View]。
     *
     * 也就是说，如果 [finishSlide] 的 [SlideDirection] 是 [SlideDirection.Origin] ，
     * 也就是滑动回弹到本来的界面上，是不会触发 [onViewComplete] 的。
     *
     * 在这个方法中实现当 [View] 第一次完全出现时才做的业务。比如开始播放视频。
     *
     * @param viewHolder 持有 [View] 的 [SlideViewHolder]
     */
    fun onViewComplete(viewHolder: ViewHolder, direction: SlideDirection) {}

    /**
     * 当滑动完成时，离开的 [View] 会触发，在这个方法中实现对 [View] 的清理。
     *
     * @param viewHolder 持有 [View] 的 [SlideViewHolder]
     * @param direction  滑动的方向
     */
    fun onViewDismiss(viewHolder: ViewHolder, parent: ViewGroup, direction: SlideDirection) {}

    /**
     * 当滑动完成时触发。
     *
     * @param direction 滑动的方向
     */
    fun finishSlide(dismissViewHolder: ViewHolder, visibleViewHolder: ViewHolder, direction: SlideDirection) {}
}

open class SlideViewHolder(val view: View)

abstract class SlideViewAdapter : SlideAdapter<SlideViewHolder> {

    /**
     * 创建 [View] 。
     * 一般来说，该方法会在 [SlidableLayout.setAdapter] 方法调用时触发一次，创建当前显示的 [View]，
     * 会在首次开始滑动时触发第二次，创建滑动目标的 [View]。
     */
    protected abstract fun onCreateView(context: Context, parent: ViewGroup, inflater: LayoutInflater): View

    /**
     * 当 [view] 开始滑动到可见时触发，在这个方法中实现数据和 [view] 的绑定。
     *
     * @param direction  滑动的方向
     */
    protected abstract fun onBindView(view: View, direction: SlideDirection)

    /**
     * 当滑动完成时触发。
     *
     * @param direction 滑动的方向
     */
    protected open fun finishSlide(direction: SlideDirection) {}

    /**
     * 当滑动完成时触发。
     *
     * @param direction 滑动的方向
     */
    protected open fun finishSlide(dismissView: View, visibleView: View, direction: SlideDirection) {}

    /**
     * 当滑动完成时，离开的 [view] 会触发，在这个方法中实现对 [view] 的清理。
     *
     * @param direction  滑动的方向
     */
    protected open fun onViewDismiss(view: View, parent: ViewGroup, direction: SlideDirection) {
        parent.removeView(view)
    }

    /**
     * 当 [view] 完全出现时触发。
     * 这个时机可能是 [SlidableLayout.setAdapter] 后 [view] 的第一次初始化，
     * 也可能是完成一次滑动，在 [finishSlide] 后 **而且** 滑到了一个新的 [view]。
     *
     * 也就是说，如果 [finishSlide] 的 [SlideDirection] 是 [SlideDirection.Origin] ，
     * 也就是滑动回弹到本来的界面上，是不会触发 [onViewComplete] 的。
     *
     * 在这个方法中实现当 [view] 第一次完全出现时才做的业务。比如开始播放视频。
     */
    protected open fun onViewComplete(view: View, direction: SlideDirection) {}

    final override fun onCreateViewHolder(context: Context, parent: ViewGroup, inflater: LayoutInflater): SlideViewHolder {
        return SlideViewHolder(onCreateView(context, parent, inflater))
    }

    final override fun onBindView(viewHolder: SlideViewHolder, direction: SlideDirection) {
        val v = viewHolder.view
        onBindView(v, direction)
        if (v is SlidableUI) {
            v.startVisible(direction)
        }
    }

    final override fun onViewDismiss(viewHolder: SlideViewHolder, parent: ViewGroup, direction: SlideDirection) {
        val v = viewHolder.view
        if (v is SlidableUI) {
            v.invisible(direction)
        }
        onViewDismiss(v, parent, direction)
    }

    final override fun onViewComplete(viewHolder: SlideViewHolder, direction: SlideDirection) {
        val v = viewHolder.view
        onViewComplete(v, direction)
        if (v is SlidableUI) {
            v.invisible(direction)
        }
    }

    final override fun finishSlide(dismissViewHolder: SlideViewHolder, visibleViewHolder: SlideViewHolder, direction: SlideDirection) {
        finishSlide(direction)
        finishSlide(dismissViewHolder.view, visibleViewHolder.view, direction)
        if (dismissViewHolder.view is SlidableUI) {
            dismissViewHolder.view.preload(direction)
        }
    }
}

abstract class SlideFragmentAdapter(private val fm: FragmentManager) : SlideAdapter<FragmentViewHolder> {

    private val viewHolderList = mutableListOf<FragmentViewHolder>()

    abstract fun onCreateFragment(context: Context): Fragment

    protected open fun onBindFragment(fragment: Fragment, direction: SlideDirection) {}

    protected open fun finishSlide(direction: SlideDirection) {}

    final override fun onCreateViewHolder(context: Context, parent: ViewGroup, inflater: LayoutInflater): FragmentViewHolder {
        val viewGroup = FrameLayout(context)
        viewGroup.id = ViewCompat.generateViewId()
        val fragment = onCreateFragment(context)
        fm.beginTransaction().add(viewGroup.id, fragment).commitAllowingStateLoss()
        val viewHolder = FragmentViewHolder(viewGroup, fragment)
        viewHolderList.add(viewHolder)
        return viewHolder
    }

    final override fun onBindView(viewHolder: FragmentViewHolder, direction: SlideDirection) {
        val fragment = viewHolder.f
        fm.beginTransaction().show(fragment).commitAllowingStateLoss()
        viewHolder.view.post {
            onBindFragment(fragment, direction)
            if (fragment is SlidableUI) {
                fragment.startVisible(direction)
            }
        }
    }

    final override fun onViewComplete(viewHolder: FragmentViewHolder, direction: SlideDirection) {
        val fragment = viewHolder.f
        fragment.setMenuVisibility(true)
        fragment.userVisibleHint = true
        if (fragment is SlidableUI) {
            fragment.completeVisible(direction)
        }

        viewHolderList.filter { it != viewHolder }.forEach {
            val otherFragment = it.f
            otherFragment.setMenuVisibility(false)
            otherFragment.userVisibleHint = false
        }
    }

    final override fun onViewDismiss(viewHolder: FragmentViewHolder, parent: ViewGroup, direction: SlideDirection) {
        val fragment = viewHolder.f
        fm.beginTransaction().hide(fragment).commitAllowingStateLoss()
        if (fragment is SlidableUI) {
            fragment.invisible(direction)
        }
    }

    final override fun finishSlide(dismissViewHolder: FragmentViewHolder, visibleViewHolder: FragmentViewHolder, direction: SlideDirection) {
        finishSlide(direction)
        if (dismissViewHolder.f is SlidableUI) {
            dismissViewHolder.f.preload(direction)
        }
    }
}

class FragmentViewHolder(v: ViewGroup, val f: Fragment) : SlideViewHolder(v)

enum class SlideDirection : SlideIndexer {
    /**
     * 滑到下一个
     */
    Next {
        override fun moveTo(index: Int): Int = index + 1
    },
    /**
     * 滑到上一个
     */
    Prev {
        override fun moveTo(index: Int): Int = index - 1
    },
    /**
     * 回到原点
     */
    Origin {
        override fun moveTo(index: Int): Int = index
    }
}

interface SlideIndexer {

    /**
     * 计算index的变化
     */
    fun moveTo(index: Int): Int
}

enum class SlideAction {
    /**
     * 正常滑动切换页面
     */
    Slide,
    /**
     * 加载中（未实现）
     */
    Load,
    /**
     * 无法滑动
     */
    Freeze
}

interface SlidableUI {

    /**
     * 滑动开始，页面可见
     */
    fun startVisible(direction: SlideDirection) {}

    /**
     * 滑动结束时，页面完全可见
     */
    fun completeVisible(direction: SlideDirection) {}

    /**
     * 滑动结束时，页面完全不可见
     */
    fun invisible(direction: SlideDirection) {}

    /**
     * 滑动完全结束，可以预加载下一个页面
     */
    fun preload(direction: SlideDirection) {}
}