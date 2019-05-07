package com.yy.mobile.slidablelayout

import android.os.Handler
import android.os.Looper

/**
 * Created by 张宇 on 2019/5/6.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class PageInfoRepository {

    private val fakeRemoteResource = listOf(
        PageInfo(R.drawable.a, "接受挑战"),
        PageInfo(R.drawable.b, "钢铁侠指日可待"),
        PageInfo(R.drawable.c, "才知道原来有这个功能"),
        PageInfo(R.drawable.d, "google拍下摔跤瞬间"),
        PageInfo(R.drawable.e, "惊天一锤"),
        PageInfo(R.drawable.f, "为了吃提高智商"),
        PageInfo(R.drawable.g, "趁没人注意赶紧走"),
        PageInfo(R.drawable.h, "可以说明质量好多了")
    )

    private val handler = Handler(Looper.getMainLooper())

    /**
     * get the `pageInfo` list in the range of [offset,offset+size)
     */
    fun requestPageInfo(
        offset: Int,
        size: Int,
        delayMills: Long = 0,
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

        if (delayMills > 0) {
            handler.postDelayed({
                callback(result, isLastPage)
            }, delayMills)
        } else {
            callback(result, isLastPage)
        }
    }
}