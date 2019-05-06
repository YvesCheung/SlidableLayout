package com.yy.mobile.slidablelayout

import android.graphics.Color

/**
 * Created by 张宇 on 2019/5/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class PageInfoRepository {

    private val fakeRemoteResource = listOf(
        PageInfo(Color.BLUE, R.drawable.a, "我是第一个"),
        PageInfo(Color.CYAN, R.drawable.b, "我是第二个"),
        PageInfo(Color.GRAY, R.drawable.c, "我是第三个"),
        PageInfo(Color.RED, R.drawable.d, "我是第四个"),
        PageInfo(Color.GREEN, R.drawable.a, "我是第五个"),
        PageInfo(Color.YELLOW, R.drawable.b, "我是第六个"),
        PageInfo(Color.WHITE, R.drawable.c, "我是第七个"),
        PageInfo(Color.BLACK, R.drawable.d, "我是第八个")
    )

    /**
     * get the `pageInfo` list in the range of [offset,offset+size)
     */
    fun requestPageInfo(
        offset: Int,
        size: Int,
        callback: (result: List<PageInfo>, isLastPage: Boolean) -> Unit
    ) {
        var isLastPage = true
        var result: List<PageInfo> = listOf()

        if (offset < fakeRemoteResource.size) {
            if (offset + size <= fakeRemoteResource.size) {
                result = fakeRemoteResource.subList(offset, offset + size).toList()
                isLastPage = false
            } else {
                result = fakeRemoteResource.subList(offset, fakeRemoteResource.size).toList()
            }
        }
        callback(result, isLastPage)
    }
}