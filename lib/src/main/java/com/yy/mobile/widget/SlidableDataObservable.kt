package com.yy.mobile.widget

import android.database.Observable

/**
 * @author YvesCheung
 * 2020-02-22
 */
internal class SlidableDataObservable : Observable<SlidableDataObserver>() {

    fun notifyDataSetChanged() {
        val copyList =
            synchronized(mObservers) {
                mObservers.toList()
            }
        copyList.forEach { it.onChanged() }
    }
}