package com.yy.mobile.slidablelayout

import com.yy.mobile.widget.SlidableLayout
import kotlinx.android.synthetic.main.activity_demo.*

/**
 * @author YvesCheung
 * 2020-02-22
 */
class DemoForDataSetChanged : DemoForFragment() {

    override val dataList: SimpleListQueue<PageInfo> = IndexedListQueue()

    override val pageSize: Int = 1

    override fun requestDataAndAddToAdapter(insertToFirst: Boolean, delayMills: Long) {
        repo.requestPageInfo(offset, pageSize, delayMills) { result, isLastPage ->
            if (insertToFirst) {
                dataList.addFirst(result)
                /**
                 * Note: add this code to refresh the [SlidableLayout]
                 */
                slidable_layout.notifyDataSetChanged()
                refresh_layout.finishRefresh(0, true, isLastPage)
            } else {
                dataList.addLast(result)
                refresh_layout.finishLoadMore(0, true, isLastPage)
            }
            offset += result.size
        }
    }

    private open class IndexedListQueue<Element> : SimpleListQueue<Element>() {

        override fun addFirst(data: List<Element>) {
            actual.addAll(0, data)
        }
    }
}