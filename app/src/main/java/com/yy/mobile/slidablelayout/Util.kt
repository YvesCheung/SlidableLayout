package com.yy.mobile.slidablelayout

import android.app.Activity
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition

/**
 * Created by 张宇 on 2019/5/7.
 * E-mail: zhangyu4@yy.com
 * YY: 909017428
 */
fun ImageView.setGifResource(@DrawableRes resId: Int) {
    val view = this
    val ctx = view.context as? Activity
    if (ctx == null ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && ctx.isDestroyed)) {
        return
    }
    Glide.with(view).load(resId).into(object : SimpleTarget<Drawable>() {
        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            view.setImageDrawable(resource)
            if (view.getTag(R.id.completeVisible) == true) {
                view.startAnimation()
            }
        }
    })
}

fun ImageView.startAnimation() {
    val d = this.drawable
    if (d is Animatable) {
        d.start()
    }
}