package com.yy.mobile.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

abstract class SlideFragmentAdapter(private val fm: FragmentManager) : SlideAdapter<FragmentViewHolder> {

    private val viewHolderList = mutableListOf<FragmentViewHolder>()

    /**
     * Called by [SlidableLayout] to create the content [Fragment].
     *
     * ————————————————————————————————————————————————————————————————————————————————
     * 创建要显示的 [Fragment]。
     * 一般来说，该方法会在 [SlidableLayout.setAdapter] 调用时触发一次，创建当前显示的 [Fragment]，
     * 会在首次开始滑动时触发第二次，创建滑动目标的 [Fragment]。
     */
    abstract fun onCreateFragment(context: Context): Fragment

    protected open fun onBindFragment(fragment: Fragment, direction: SlideDirection) {}

    protected open fun finishSlide(direction: SlideDirection) {}

    final override fun onCreateViewHolder(context: Context, parent: ViewGroup, inflater: LayoutInflater): FragmentViewHolder {
        val viewGroup = FrameLayout(context)
        viewGroup.id = ViewCompat.generateViewId()
        val fragment = onCreateFragment(context)
        fm.beginTransaction().add(viewGroup.id, fragment).commitAllowingStateLoss()
        val viewHolder = FragmentViewHolder(viewGroup, fragment)
        viewHolderList.add(viewHolder)
        return viewHolder
    }

    final override fun onBindView(viewHolder: FragmentViewHolder, direction: SlideDirection) {
        val fragment = viewHolder.f
        fm.beginTransaction().show(fragment).commitAllowingStateLoss()
        viewHolder.view.post {
            onBindFragment(fragment, direction)
            if (fragment is SlidableUI) {
                fragment.startVisible(direction)
            }
        }
    }

    final override fun onViewComplete(viewHolder: FragmentViewHolder, direction: SlideDirection) {
        val fragment = viewHolder.f
        fragment.setMenuVisibility(true)
        fragment.userVisibleHint = true
        if (fragment is SlidableUI) {
            fragment.completeVisible(direction)
        }

        viewHolderList.filter { it != viewHolder }.forEach {
            val otherFragment = it.f
            otherFragment.setMenuVisibility(false)
            otherFragment.userVisibleHint = false
        }
    }

    final override fun onViewDismiss(viewHolder: FragmentViewHolder, parent: ViewGroup, direction: SlideDirection) {
        val fragment = viewHolder.f
        fm.beginTransaction().hide(fragment).commitAllowingStateLoss()
        if (fragment is SlidableUI) {
            fragment.invisible(direction)
        }
    }

    final override fun finishSlide(dismissViewHolder: FragmentViewHolder, visibleViewHolder: FragmentViewHolder, direction: SlideDirection) {
        finishSlide(direction)
        if (dismissViewHolder.f is SlidableUI) {
            dismissViewHolder.f.preload(direction)
        }
    }
}