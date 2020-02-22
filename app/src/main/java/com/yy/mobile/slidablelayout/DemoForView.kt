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
 * Created by 张宇 on 2019/5/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
open class DemoForView : BaseDemoActivity() {

    override fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder> =
        DemoViewAdapter(data)

    open class DemoViewAdapter(protected val data: SimpleQueue<PageInfo>) : SlideViewAdapter() {

        @Suppress("CascadeIf")
        override fun canSlideTo(direction: SlideDirection): Boolean {
            val info =
                if (direction == SlideDirection.Next) {
                    data.next()
                } else if (direction == SlideDirection.Prev) {
                    data.prev()
                } else {
                    data.current()
                }
            return info != null
        }

        @SuppressLint("InflateParams")
        override fun onCreateView(context: Context, parent: ViewGroup, inflater: LayoutInflater): View {
            return inflater.inflate(R.layout.page_main_content, null, false)
        }

        override fun onBindView(view: View, direction: SlideDirection) {
            val info =
                if (direction == SlideDirection.Next) {
                    data.next()!!
                } else if (direction == SlideDirection.Prev) {
                    data.prev()!!
                } else {
                    data.current()!!
                }
            view.content_title.text = info.title
            view.content_player.setImageDrawable(null) //should be snapshot
            view.content_player.setGifResource(info.drawableRes)
        }

        override fun onViewDismiss(view: View, parent: ViewGroup, direction: SlideDirection) {
            //clean up the resource
            view.content_player.setImageDrawable(null)
            view.content_player.setTag(R.id.completeVisible, false)
            super.onViewDismiss(view, parent, direction)
        }

        override fun finishSlide(direction: SlideDirection) {
            if (direction == SlideDirection.Next) {
                data.moveToNext()
            } else if (direction == SlideDirection.Prev) {
                data.moveToPrev()
            }
        }

        override fun onViewComplete(view: View, direction: SlideDirection) {
            view.content_player.startAnimation()
            view.content_player.setTag(R.id.completeVisible, true)
        }
    }
}