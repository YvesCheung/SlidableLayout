package com.yy.mobile.widget

interface SlidableUI {

    /**
     * At the beginning of the slide, the current view will be visible.
     * Binding data into view can be implemented in this callback,
     * such as displaying place holder pictures.
     *
     * ——————————————————————————————————————————————————————————————————————————
     * 滑动开始，当前视图将要可见
     * 可以在该回调中实现数据与视图的绑定，比如显示占位的图片
     */
    fun startVisible(direction: SlideDirection) {}

    /**
     * After sliding, the current view is completely visible.
     * You can start the main business in this callback,
     * such as starting to play video, page exposure statistics...
     *
     * ——————————————————————————————————————————————————————————————————————————
     * 滑动完成，当前视图完全可见
     * 可以在该回调中开始主业务，比如开始播放视频，比如广告曝光统计
     */
    fun completeVisible(direction: SlideDirection) {}

    /**
     * After sliding, the current view is completely invisible.
     * You can do some cleaning work in this callback,
     * such as closing the video player.
     *
     * ——————————————————————————————————————————————————————————————————————————
     * 滑动完成，当前视图完全不可见
     * 可以在该回调中做一些清理工作，比如关闭播放器
     */
    fun invisible(direction: SlideDirection) {}

    /**
     *
     * Have completed a sliding in the direction, and the user is likely to
     * continue sliding in the same direction. You can preload the next page in this callback,
     * such as download the next video or prepare the cover image.
     *
     * ——————————————————————————————————————————————————————————————————————————
     * 已经完成了一次 direction 方向的滑动，用户很可能会在这个方向上继续滑动
     * 可以在该回调中实现下一次滑动的预加载，比如开始下载下一个视频或者准备好封面图
     */
    fun preload(direction: SlideDirection) {}
}