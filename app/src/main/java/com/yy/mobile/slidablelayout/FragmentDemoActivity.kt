package com.yy.mobile.slidablelayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yy.mobile.widget.SlidableLayout
import com.yy.mobile.widget.SlideAction
import com.yy.mobile.widget.SlideDirection
import com.yy.mobile.widget.SlideFragmentAdapter
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by 张宇 on 2019/4/15.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class FragmentDemoActivity : FragmentActivity() {

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

    private var text: String = ""

    private var currentIdx = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = intent.getStringExtra("text")

        val slidableLayout = SlidableLayout(this)
        setContentView(slidableLayout)

        slidableLayout.setAdapter(object : SlideFragmentAdapter(supportFragmentManager) {

            override fun onCreateFragment(context: Context): Fragment = DemoFragment()

            override fun onBindFragment(fragment: Fragment, direction: SlideDirection) {
                super.onBindFragment(fragment, direction)
                val info = list[direction.moveTo(currentIdx)]
                fragment as DemoFragment
                fragment.setBg(info.background)
                fragment.setText("${info.text} from $text")
            }

            override fun canSlideTo(direction: SlideDirection): SlideAction {
                val targetIdx = direction.moveTo(currentIdx)
                return if (targetIdx in 0 until list.size) {
                    SlideAction.Slide
                } else {
                    SlideAction.Freeze
                }
            }

            override fun finishSlide(direction: SlideDirection) {
                currentIdx = direction.moveTo(currentIdx)
            }
        })
    }

    class DemoFragment : Fragment() {

        private var currentText: String = ""

        @SuppressLint("InflateParams")
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.activity_main, null, false)
        }

        fun setBg(color: Int) {
            rl_bg.setBackgroundColor(color)
        }

        fun setText(text: String) {
            currentText = text
            tv_desc.text = text
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            Log.i("Yves", "onCreate $currentText")
        }

        override fun onHiddenChanged(hidden: Boolean) {
            super.onHiddenChanged(hidden)
            Log.i("Yves", "onHiddenChanged $hidden $currentText")
        }

        override fun onResume() {
            super.onResume()
            Log.i("Yves", "onResume $currentText")
        }

        override fun onStop() {
            super.onStop()
            Log.i("Yves", "onStop $currentText")
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.i("Yves", "onDestroy $currentText")
        }

        override fun setUserVisibleHint(isVisibleToUser: Boolean) {
            super.setUserVisibleHint(isVisibleToUser)
            Log.i("Yves", "setUserVisibleHint $isVisibleToUser $currentText")
        }
    }
}