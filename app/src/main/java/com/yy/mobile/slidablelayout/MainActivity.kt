package com.yy.mobile.slidablelayout

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideViewAdapter
import kotlinx.android.synthetic.main.activity_demo.*
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
        setContentView(R.layout.activity_demo)

        refresh_layout.setEnableRefresh(true)
        refresh_layout.setEnableLoadMore(true)
        refresh_layout.setRefreshHeader(ClassicsHeader(this))
        refresh_layout.setRefreshFooter(ClassicsFooter(this))

        slidable_layout.setAdapter(object : SlideViewAdapter() {
            override fun canSlideTo(direction: SlideDirection): Boolean {
                val targetIdx = direction.moveTo(currentIdx)
                return targetIdx in 0 until list.size
            }

            @SuppressLint("InflateParams")
            override fun onCreateView(context: Context, parent: ViewGroup, inflater: LayoutInflater): View {
                return inflater.inflate(R.layout.activity_main, null, false)
            }

            override fun onBindView(view: View, direction: SlideDirection) {
                val info = list[direction.moveTo(currentIdx)]
                Log.i("Yves", "onBindView = $info")
                view.rl_bg.setBackgroundColor(info.background)
                view.tv_desc.text = info.text
                view.btn_i_am.setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, FragmentDemoActivity::class.java)
                            .putExtra("text", info.text)
                    )
                }
            }

            override fun onViewDismiss(view: View, parent: ViewGroup, direction: SlideDirection) {
                super.onViewDismiss(view, parent, direction)
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
}
