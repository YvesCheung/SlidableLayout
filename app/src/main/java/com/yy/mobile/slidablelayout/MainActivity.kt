package com.yy.mobile.slidablelayout

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun toDemoForFragment(v: View) {
        startActivity(Intent(this, DemoForFragment::class.java))
    }

    fun toDemoForView(v: View) {
        startActivity(Intent(this, DemoForView::class.java))
    }

    fun toDemoForLoop(v: View) {
        startActivity(Intent(this, DemoForLoop::class.java))
    }

    fun toDemoForLoopHorizontally(v: View) {
        startActivity(Intent(this, DemoForLoopHorizontally::class.java))
    }

    fun toDemoForAutoSlide(v: View) {
        startActivity(Intent(this, DemoForAutoSlide::class.java))
    }

    fun toDemoForDataSetChanged(v: View) {
        startActivity(Intent(this, DemoForDataSetChanged::class.java))
    }

    fun toDemoForNestedScroll(v: View) {
        startActivity(Intent(this, DemoForNestedScroll::class.java))
    }

    fun toDemoForCrossScroll(v: View) {
        startActivity(Intent(this, DemoForCrossScroll::class.java))
    }
}
