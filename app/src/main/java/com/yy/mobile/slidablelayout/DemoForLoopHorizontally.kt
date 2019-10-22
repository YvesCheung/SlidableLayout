package com.yy.mobile.slidablelayout

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_demo_horizontally.*

/**
 * Created by 张宇 on 2019-10-22.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
class DemoForLoopHorizontally : DemoForLoop() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_horizontally)
        slidable_layout_horizontal.setAdapter(createAdapter(dataList))
        requestDataAndAddToAdapter(false)
    }
}