package com.yy.mobile.slidablelayout

import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes

data class PageInfo(
    @ColorInt
    val background: Int,
    @DrawableRes
    val drawableRes: Int,
    val title: String
)
