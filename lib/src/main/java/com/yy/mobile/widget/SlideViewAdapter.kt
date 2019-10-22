package com.yy.mobile.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Base class for an Adapter.
 */
abstract class SlideViewAdapter : SlideAdapter<SlideViewHolder> {

    /**
     * Called by [SlidableLayout] to create the view.
     * Generally speaking, this method will be called, once the
     * [SlidableLayout.setAdapter] method is called, to create the
     * currently [View].
     * The second time this method is called is when the first sliding
     * event occurs, to create the sliding target's [View].
     *
     * ———————————————————————————————————————————————————————————————————————————
     * 创建 [View] 。
     * 一般来说，该方法会在 [SlidableLayout.setAdapter] 方法调用时触发一次，
     * 创建当前显示的 [View]，
     * 会在首次开始滑动时触发第二次，创建滑动目标的 [View]。
     */
    protected abstract fun onCreateView(context: Context, parent: ViewGroup, inflater: LayoutInflater): View

    /**
     * Called by [SlidableLayout] when [view] starts to slide to visible.
     * This method should update the contents of the [view] to reflect the
     * item at the [direction].
     *
     * ——————————————————————————————————————————————————————————————————————————————
     * 当 [view] 开始滑动到可见时触发，在这个方法中实现数据和 [view] 的绑定。
     *
     * @param direction  滑动的方向
     */
    protected abstract fun onBindView(view: View, direction: SlideDirection)

    /**
     * Called by [SlidableLayout] when view finishes sliding.
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 当滑动完成时触发。
     *
     * @param direction 滑动的方向
     */
    protected open fun finishSlide(direction: SlideDirection) {}

    /**
     * Called by [SlidableLayout] when view finishes sliding.
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 当滑动完成时触发。
     *
     * @param direction 滑动的方向
     */
    protected open fun finishSlide(dismissView: View, visibleView: View, direction: SlideDirection) {}

    /**
     * Called by [SlidableLayout] when a view created by this
     * adapter has been dismissed.
     * Do some cleaning in this method.
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 当滑动完成时，离开的 [view] 会触发，在这个方法中实现对 [view] 的清理。
     *
     * @param direction  滑动的方向
     */
    protected open fun onViewDismiss(view: View, parent: ViewGroup, direction: SlideDirection) {
        parent.removeView(view)
    }

    /**
     * Called by [SlidableLayout] when [view] completely appears.
     *
     * ——————————————————————————————————————————————————————————————————————————————————————
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
            v.completeVisible(direction)
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