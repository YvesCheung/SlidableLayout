package com.yy.mobile.slidablelayout

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yy.mobile.widget.SlidableLayout
import com.yy.mobile.widget.SlidableLayout.Companion.HORIZONTAL
import com.yy.mobile.widget.SlideAdapter
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideViewAdapter
import com.yy.mobile.widget.SlideViewHolder

/**
 * @author YvesCheung
 * 2020-02-23
 */
class DemoForCrossScroll : BaseDemoActivity() {

    override fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder> {
        return OuterScrollAdapter(data)
    }

    private class OuterScrollAdapter(
        val data: SimpleQueue<PageInfo>
    ) : SlideViewAdapter() {

        override fun onCreateView(context: Context, parent: ViewGroup, inflater: LayoutInflater): View =
            SlidableLayout(context).apply {
                orientation = HORIZONTAL
                setAdapter(InnerScrollAdapter(data))
            }

        override fun onBindView(view: View, direction: SlideDirection) {}

        override fun canSlideTo(direction: SlideDirection): Boolean = true
    }

    private class InnerScrollAdapter(data: SimpleQueue<PageInfo>) : DemoForLoop.LoopAdapter(data)
}