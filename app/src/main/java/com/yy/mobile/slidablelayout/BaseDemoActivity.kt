package com.yy.mobile.slidablelayout

import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import com.yy.mobile.widget.SlideAdapter
import com.yy.mobile.widget.SlideViewHolder
import kotlinx.android.synthetic.main.activity_demo.*

/**
 * Created by 张宇 on 2019/5/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
abstract class BaseDemoActivity : FragmentActivity() {

    private val dataList = SimpleListQueue<PageInfo>()

    private val repo = PageInfoRepository()

    private var offset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive()
        setContentView(R.layout.activity_demo)

        requestDataAndAddToAdapter(false)

        initRefreshLayout()

        slidable_layout.setAdapter(createAdapter(dataList))
    }

    /**
     * 全屏
     */
    private fun immersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    /**
     * 这里可替换为任意支持NestedScroll的刷新布局
     */
    private fun initRefreshLayout() {
        refresh_layout
            .setEnableNestedScroll(true)
            .setEnableRefresh(true)
            .setEnableLoadMore(true)
            .setRefreshHeader(ClassicsHeader(this))
            .setRefreshFooter(ClassicsFooter(this))
            .setOnRefreshListener {
                requestDataAndAddToAdapter(true, 1000L)
            }
            .setOnLoadMoreListener {
                requestDataAndAddToAdapter(false, 1000L)
            }
    }

    private fun requestDataAndAddToAdapter(insertToFirst: Boolean = true, delayMills: Long = 0L) {
        repo.requestPageInfo(offset, pageSize, delayMills) { result, isLastPage ->
            if (insertToFirst) {
                dataList.addFirst(result)
                refresh_layout.finishRefresh(0, true, isLastPage)
            } else {
                dataList.addLast(result)
                refresh_layout.finishLoadMore(0, true, isLastPage)
            }
            offset += result.size
        }
    }

    protected open val pageSize: Int get() = 4

    protected abstract fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder>

    private class SimpleListQueue<Element>(
        private val actual: MutableList<Element> = mutableListOf()
    ) : SimpleQueue<Element>, List<Element> by actual {

        private var curIdx = 0

        fun addFirst(data: List<Element>) {
            actual.addAll(0, data)
            curIdx += data.size
        }

        fun addLast(data: List<Element>) {
            actual.addAll(data)
        }

        override fun next(): Element? {
            return actual.getOrNull(curIdx + 1)
        }

        override fun current(): Element? {
            return actual.getOrNull(curIdx)
        }

        override fun moveToNext() {
            curIdx++
        }

        override fun prev(): Element? {
            return actual.getOrNull(curIdx - 1)
        }

        override fun moveToPrev() {
            curIdx--
        }

        fun moveTo(idx: Int) {
            if (idx in 0 until actual.size) {
                curIdx = idx
            } else {
                throw IndexOutOfBoundsException("index must between 0 and ${actual.size} " +
                    "but now is $idx")
            }
        }
    }
}