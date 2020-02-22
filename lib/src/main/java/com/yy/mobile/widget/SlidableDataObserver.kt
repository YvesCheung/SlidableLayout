package com.yy.mobile.widget

/**
 * Receives call backs when a data set has been changed.
 *
 * @author YvesCheung
 * 2020-02-22
 */
interface SlidableDataObserver {

    /**
     * This method is called when the entire data set has changed
     */
    fun onChanged()
}