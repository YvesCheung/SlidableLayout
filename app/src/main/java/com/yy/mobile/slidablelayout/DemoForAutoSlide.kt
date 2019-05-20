package com.yy.mobile.slidablelayout

import android.os.Handler
import android.os.Looper
import android.view.View
import com.yy.mobile.widget.SlideAdapter
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideViewHolder
import kotlinx.android.synthetic.main.activity_demo.*

/**
 * Created by 张宇 on 2019/5/20.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class DemoForAutoSlide : BaseDemoActivity() {

    override fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder> =
        AutoSlideAdapter(data)

    override val pageSize: Int get() = 8

    private var autoSlideDirection = SlideDirection.Next

    private val handler = Handler(Looper.getMainLooper())

    /**
     * 视图出现的两秒后，自动滑到下一个视图
     */
    private inner class AutoSlideAdapter(data: SimpleQueue<PageInfo>) : DemoForView.DemoViewAdapter(data) {

        override fun onViewComplete(view: View, direction: SlideDirection) {
            super.onViewComplete(view, direction)
            handler.removeCallbacks(autoSlide)
            handler.postDelayed(autoSlide, 2000L)
        }
    }

    private val autoSlide = Runnable {
        if (!slidable_layout.slideTo(autoSlideDirection)) {
            //如果当前方向不能再滑了，往反方向滑
            autoSlideDirection = toggle()
            slidable_layout.slideTo(autoSlideDirection)
        }
    }

    private fun toggle(): SlideDirection {
        return if (autoSlideDirection == SlideDirection.Next) {
            SlideDirection.Prev
        } else {
            SlideDirection.Next
        }
    }
}