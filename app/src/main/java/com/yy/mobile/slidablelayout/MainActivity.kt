package com.yy.mobile.slidablelayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.yy.mobile.widget.SlidableLayout
import com.yy.mobile.widget.SlideAction
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideViewAdapter
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivity : AppCompatActivity() {

    private val list = listOf(
        ViewInfo(Color.BLUE, "blue"),
        ViewInfo(Color.CYAN, "CYAN"),
        ViewInfo(Color.GRAY, "GRAY"),
        ViewInfo(Color.RED, "RED"),
        ViewInfo(Color.GREEN, "GREEN"),
        ViewInfo(Color.YELLOW, "YELLOW"),
        ViewInfo(Color.WHITE, "WHITE"),
        ViewInfo(Color.BLACK, "BLACK")
    )

    private var currentIdx = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val slidableLayout = SlidableLayout(this)
        setContentView(slidableLayout)

        slidableLayout.setAdapter(object : SlideViewAdapter() {
            override fun canSlideTo(direction: SlideDirection): SlideAction {
                val targetIdx = direction.moveTo(currentIdx)
                return if (targetIdx in 0 until list.size) {
                    SlideAction.Slide
                } else {
                    SlideAction.Freeze
                }
            }

            @SuppressLint("InflateParams")
            override fun onCreateView(context: Context, inflater: LayoutInflater): View {
                return inflater.inflate(R.layout.activity_main, null, false)
            }

            override fun onBindView(view: View, direction: SlideDirection) {
                val info = list[direction.moveTo(currentIdx)]
                Log.i("Yves", "onBindView = $info")
                view.rl_bg.setBackgroundColor(info.background)
                view.tv_desc.text = info.text
                view.btn_i_am.setOnClickListener {
                    Toast.makeText(this@MainActivity, info.text, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onViewDismiss(view: View, direction: SlideDirection) {
                view.tv_desc.setTextColor(Color.BLACK)
                view.tv_desc.setBackgroundColor(Color.TRANSPARENT)
            }

            override fun finishSlide(direction: SlideDirection) {
                currentIdx = direction.moveTo(currentIdx)
            }

            override fun onViewComplete(view: View, direction: SlideDirection) {
                view.tv_desc.setBackgroundColor(Color.parseColor("#ffff00dd"))
                view.tv_desc.setTextColor(Color.WHITE)
            }
        })
    }

    data class ViewInfo(val background: Int, val text: String)
}
