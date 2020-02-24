package com.yy.mobile.slidablelayout

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.VERTICAL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.yy.mobile.widget.SlideAdapter
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideViewAdapter
import com.yy.mobile.widget.SlideViewHolder
import kotlinx.android.synthetic.main.activity_demo.*
import java.util.*

/**
 * @author YvesCheung
 * 2020-02-18
 */
class DemoForNestedScroll : BaseDemoActivity() {

    private val orientation = VERTICAL //HORIZONTAL also ok

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slidable_layout.orientation = orientation
    }

    override fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder> {
        return NestedScrollingAdapter(orientation)
    }

    open class NestedScrollingAdapter(private val orientation: Int) : SlideViewAdapter() {

        override fun onCreateView(
            context: Context,
            parent: ViewGroup,
            inflater: LayoutInflater
        ): View {
            val backgroundColor = Random().nextInt() or 0xFF000000.toInt()
            return RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context, orientation, false)
                adapter = RecyclerViewAdapter()
                setBackgroundColor(backgroundColor)
            }
        }

        override fun onBindView(view: View, direction: SlideDirection) {
            //Do Nothing.
        }

        override fun canSlideTo(direction: SlideDirection): Boolean = true
    }

    private class RecyclerViewAdapter : RecyclerView.Adapter<Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): Holder =
            Holder(TextView(parent.context))

        override fun getItemCount(): Int = 30

        override fun onBindViewHolder(viewHolder: Holder, position: Int) {
            viewHolder.textView.text = position.toString()
            viewHolder.textView.setTextColor(Color.BLACK)
            viewHolder.textView.textSize = 25f
            viewHolder.textView.setPadding(20, 20, 20, 20)
        }
    }

    private class Holder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}