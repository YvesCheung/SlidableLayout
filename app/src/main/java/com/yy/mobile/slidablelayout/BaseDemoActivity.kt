package com.yy.mobile.slidablelayout

import android.os.Bundle
import android.support.v4.app.FragmentActivity
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
        setContentView(R.layout.activity_demo)

        initRefreshLayout()

        slidable_layout.setAdapter(createAdapter(dataList))

        requestDataAndAddToAdapter(false)
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
                requestDataAndAddToAdapter(true)
            }
            .setOnLoadMoreListener {
                requestDataAndAddToAdapter(false)
            }
    }

    private fun requestDataAndAddToAdapter(insertToFirst: Boolean = true) {
        repo.requestPageInfo(offset, pageSize) { result, isLastPage ->
            if (insertToFirst) {
                dataList.addFirst(result)
                refresh_layout.finishRefresh(1000, true, isLastPage)
            } else {
                dataList.addLast(result)
                refresh_layout.finishLoadMore(1000, true, isLastPage)
            }
            offset += result.size
        }
    }

    protected open val pageSize: Int get() = 2

    protected abstract fun createAdapter(data: SimpleQueue<PageInfo>): SlideAdapter<out SlideViewHolder>

    private class SimpleListQueue<Element> : SimpleQueue<Element> {

        private var curIdx = 0

        private val actual: MutableList<Element> = mutableListOf()

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
    }
}