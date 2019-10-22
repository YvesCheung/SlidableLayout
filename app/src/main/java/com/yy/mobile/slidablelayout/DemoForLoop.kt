package com.yy.mobile.slidablelayout

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yy.mobile.widget.SlideAdapter
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideViewAdapter
import com.yy.mobile.widget.SlideViewHolder
import kotlinx.android.synthetic.main.page_main_content.view.*

/**
 * Created by 张宇 on 2019/5/7.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
open class DemoForLoop : BaseDemoActivity() {

    override fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder> =
        LoopAdapter(data)

    private class LoopAdapter(val data: SimpleQueue<PageInfo>) : SlideViewAdapter() {

        private var curIdx = 0

        override fun canSlideTo(direction: SlideDirection): Boolean = true

        @SuppressLint("InflateParams")
        override fun onCreateView(context: Context, parent: ViewGroup, inflater: LayoutInflater): View {
            return inflater.inflate(R.layout.page_main_content, null, false)
        }

        override fun onBindView(view: View, direction: SlideDirection) {
            val info = data[normalize(direction.moveTo(curIdx))]
            view.content_title.text = info.title
            view.content_player.setImageDrawable(null) //should be snapshot
            view.content_player.setGifResource(info.drawableRes)
        }

        override fun onViewComplete(view: View, direction: SlideDirection) {
            view.content_player.setTag(R.id.completeVisible, true)
            view.content_player.startAnimation()
        }

        override fun onViewDismiss(view: View, parent: ViewGroup, direction: SlideDirection) {
            super.onViewDismiss(view, parent, direction)
            view.content_player.setTag(R.id.completeVisible, false)
            view.content_player.setImageDrawable(null)
        }

        override fun finishSlide(direction: SlideDirection) {
            curIdx = normalize(direction.moveTo(curIdx))
        }

        private fun normalize(newIdx: Int): Int {
            return (newIdx + data.size) % data.size
        }
    }
}