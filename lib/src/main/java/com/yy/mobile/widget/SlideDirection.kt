package com.yy.mobile.widget

/**
 * @author YvesCheung
 * 2019-10-21
 */
enum class SlideDirection {
    /**
     * move to next
     * 滑到下一个
     */
    Next {
        override fun moveTo(index: Int): Int = index + 1
    },
    /**
     * move to previous
     * 滑到上一个
     */
    Prev {
        override fun moveTo(index: Int): Int = index - 1
    },
    /**
     * back to the origin
     * 回到原点
     */
    Origin {
        override fun moveTo(index: Int): Int = index
    };

    /**
     * 计算index的变化
     */
    abstract fun moveTo(index: Int): Int
}