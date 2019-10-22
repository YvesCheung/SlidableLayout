package com.yy.mobile.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

/**
 * Adapt to the view in layout.
 *
 * If page【A】 is initialized for the first time, the callback will be:
 * - onCreateViewHolder(context, inflater)
 * - onViewComplete(viewHolder【A】)
 *
 * If sliding from page【A】 to the next page【B】, the callback will be:
 * - canSlideTo(SlideDirection.Next)
 * - onCreateViewHolder(context, inflater) (如果是首次滑动)
 * - onBindView(viewHolder【B】, SlideDirection.Next)
 * - onViewDismiss(viewHolder【A】, SlideDirection.Next)
 * - onViewComplete(viewHolder【B】)
 * - finishSlide(SlideDirection.Next)
 *
 * If sliding from page【B】 to the previous page【A】, the callback will be:
 * - canSlideTo(SlideDirection.Prev)
 * - onBindView(viewHolder【A】, SlideDirection.Prev)
 * - onViewDismiss(viewHolder【B】, SlideDirection.Prev)
 * - onViewComplete(viewHolder【A】)
 * - finishSlide(SlideDirection.Prev)
 *
 * If try sliding from page【A】 to page【B】, but do not have enough distance or speed,
 * you will rebound to page【A】, and the callback will be:
 * - canSlideTo(SlideDirection.Next)
 * - onBindView(viewHolder【B】, SlideDirection.Next)
 * - onViewDismiss(viewHolder【B】, SlideDirection.Next)
 * - finishSlide(SlideDirection.Origin)
 *
 * ——————————————————————————————————————————————————————————————————————————————
 * 适配 [SlidableLayout] 以及布局中滑动的 [View] 。
 *
 * 假如首次初始化页面【A】，触发的回调是：
 * - onCreateViewHolder(context, inflater)
 * - onViewComplete(viewHolder【A】)
 *
 * 假如从页面【A】滑动下一个页面【B】，触发的回调将会是：
 * - canSlideTo(SlideDirection.Next)
 * - onCreateViewHolder(context, inflater) (如果是首次滑动)
 * - onBindView(viewHolder【B】, SlideDirection.Next)
 * - onViewDismiss(viewHolder【A】, SlideDirection.Next)
 * - onViewComplete(viewHolder【B】)
 * - finishSlide(SlideDirection.Next)
 *
 * 假如再从页面【B】 滑动回上一个页面 【A】，触发的回调是：
 * - canSlideTo(SlideDirection.Prev)
 * - onBindView(viewHolder【A】, SlideDirection.Prev)
 * - onViewDismiss(viewHolder【B】, SlideDirection.Prev)
 * - onViewComplete(viewHolder【A】)
 * - finishSlide(SlideDirection.Prev)
 *
 * 假如从页面【A】试图滑动到页面【B】，但距离或者速度不够，所以放手后回弹到【A】，触发的回调是：
 * - canSlideTo(SlideDirection.Next)
 * - onBindView(viewHolder【B】, SlideDirection.Next)
 * - onViewDismiss(viewHolder【B】, SlideDirection.Next)
 * - finishSlide(SlideDirection.Origin)
 */
interface SlideAdapter<ViewHolder : SlideViewHolder> {

    /**
     * Whether it can slide in the direction of [direction].
     *
     * —————————————————————————————————————————————————————————————————————————
     * 能否向 [direction] 的方向滑动。
     *
     * @param direction 滑动的方向
     *
     * @return 返回 true 表示可以滑动， false 表示不可滑动。
     * 如果有嵌套其他外层滑动布局（比如下拉刷新），当且仅当返回 false 时会触发外层的嵌套滑动。
     */
    fun canSlideTo(direction: SlideDirection): Boolean

    /**
     * Called when [SlidableLayout] needs a new [ViewHolder] to represent an item.
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 创建持有 [View] 的 [SlideViewHolder] 。
     * 一般来说，该方法会在 [SlidableLayout.setAdapter] 方法调用时触发一次，创建当前显示的 [View]，
     * 会在首次开始滑动时触发第二次，创建滑动目标的 [View]。
     */
    fun onCreateViewHolder(context: Context, parent: ViewGroup, inflater: LayoutInflater): ViewHolder

    /**
     * Called by [SlidableLayout] when view represented by [viewHolder] starts to slide to visible.
     * This method should update the contents of the [viewHolder] to reflect the
     * item at the [direction].
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 当 [View] 开始滑动到可见时触发，在这个方法中实现数据和 [View] 的绑定。
     *
     * @param viewHolder 持有 [View] 的 [SlideViewHolder]
     * @param direction  滑动的方向
     */
    fun onBindView(viewHolder: ViewHolder, direction: SlideDirection)

    /**
     * Called by [SlidableLayout] when view represented by [viewHolder] completely appears.
     *
     * ——————————————————————————————————————————————————————————————————————————————
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
     * Called by [SlidableLayout] when a view created by this
     * adapter has been dismissed.
     * Do some cleaning in this method.
     *
     * ——————————————————————————————————————————————————————————————————————————————
     * 当滑动完成时，离开的 [View] 会触发，在这个方法中实现对 [View] 的清理。
     *
     * @param viewHolder 持有 [View] 的 [SlideViewHolder]
     * @param direction  滑动的方向
     */
    fun onViewDismiss(viewHolder: ViewHolder, parent: ViewGroup, direction: SlideDirection) {}

    /**
     * Called by [SlidableLayout] when view finishes sliding.
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 当滑动完成时触发。
     *
     * @param direction 滑动的方向
     */
    fun finishSlide(dismissViewHolder: ViewHolder, visibleViewHolder: ViewHolder, direction: SlideDirection) {}
}