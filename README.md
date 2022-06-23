# SlidableLayout

[![License](https://img.shields.io/badge/License%20-Apache%202-337ab7.svg)](https://www.apache.org/licenses/LICENSE-2.0)  [![](https://jitpack.io/v/YvesCheung/SlidableLayout.svg)](https://jitpack.io/#YvesCheung/SlidableLayout)

**SlidableLayout** is devoted to build a stable, easy-to-use and smooth sliding layout.

[中文版](README_CN.md)

Preview
========
|  **Vertical**   |  **Horizontal**  |
| :----:  | :----: |
| ![SlidableLayout][1]  | ![SlidableLayoutHorizontal][2] |
|  **Nested scroll**  |  **Opposite nested scroll**  |
| ![NestedScroll][3]  | ![OppositeNestedScroll][4] |

Features
========

- Support adapt to the `View` or `Fragment`
- Switch the position of two reusable `View` in turn when sliding
- Abundant callback to cover lifecycle
- Support infinite sliding
- Support `NestedScrolling` and can be used with layouts that implement the `NestedScrollingParent` interface, such as [SwipeRefreshLayout][5].

Usage
========

### write in XML
```xml
<!--vertical-->
<com.yy.mobile.widget.SlidableLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>

<!--horizontal-->
<com.yy.mobile.widget.SlidableLayout 
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal" />
```

Implements the [NestedScrollingChild][6] interface, `SlidableLayout` can nest into other refresh layouts, so you can customize your pull-down-refresh and pull-up-load-more behavior.

### finish adapter code


```kotlin
class MyAdapter(fm: FragmentManager) : SlideFragmentAdapter(fm) {

    private val data = listOf("a", "b", "c", "d")
    private var currentIndex = 0

    /**
     * Whether it can slide in the direction of [direction].
     */
    override fun canSlideTo(direction: SlideDirection): Boolean {
        val index =
            when (direction) {
                SlideDirection.Next -> currentIndex + 1 //can slide to next page
                SlideDirection.Prev -> currentIndex - 1 //can slide to previous page
                else -> currentIndex
            }
        return index in 0 until data.size
    }

    /**
     * Called by [SlidableLayout] to create the content [Fragment].
     */
    override fun onCreateFragment(context: Context): Fragment {
        return DemoFragment()
    }

    /**
     * Called by [SlidableLayout] when the [fragment] starts to slide to visible.
     * This method should update the contents of the [fragment] to reflect the
     * item at the [direction].
     */
    override fun onBindFragment(fragment: Fragment, direction: SlideDirection) {
        val index =
            when (direction) {
                SlideDirection.Next -> currentIndex + 1 //to next page
                SlideDirection.Prev -> currentIndex - 1 //to previous page
                SlideDirection.Origin -> currentIndex
            }
        //bind data to the ui
        (fragment as DemoFragment).currentData = data[index]
    }

    /**
     * Called by [SlidableLayout] when the view finishes sliding.
     */
    override fun finishSlide(direction: SlideDirection) {
        super.finishSlide(direction)
        //update current index
        currentIndex =
            when (direction) {
                SlideDirection.Next -> currentIndex + 1 //already to next page
                SlideDirection.Prev -> currentIndex - 1 //already to previous page
                SlideDirection.Origin -> currentIndex //rebound to origin page
            }
    }
}
```

Call `setAdapter` to add `Fragment` into `SlideableLayout` ：

```kotlin
slidable_layout.setAdapter(MyAdapter(supportFragmentManager))
```

The [demo][7] provides more detail.

### Callback

The `View` or `Fragment` created by `SlideAdapter` can implement the `SlidableUI` interface：

```kotlin
class DemoFragment : Fragment(), SlidableUI {

    override fun startVisible(direction: SlideDirection) {
        //At the beginning of the slide, the current view will be visible.
        //Binding data into view can be implemented in this callback,
        //such as displaying place holder pictures.
    }

    override fun completeVisible(direction: SlideDirection) {
        //After sliding, the current view is completely visible.
        //You can start the main business in this callback,
        //such as starting to play video, page exposure statistics...
    }

    override fun invisible(direction: SlideDirection) {
        //After sliding, the current view is completely invisible.
        //You can do some cleaning work in this callback,
        //such as closing the video player.
    }

    override fun preload(direction: SlideDirection) {
        //Have completed a sliding in the direction, and the user is likely to
        //continue sliding in the same direction. 
        //You can preload the next page in this callback,
        //such as download the next video or prepare the cover image.
    }
}
```

Install
========

1. Add it in your root build.gradle at the end of repositories:
    ```groovy
    allprojects {
    	repositories {
    		...
    		maven { url 'https://jitpack.io' }
    	}
    }
    ```

2. Add the dependency
    ```groovy
    dependencies {
        implementation 'com.github.YvesCheung:SlidableLayout:1.2.0'
    }
    ```


License
========

    Copyright 2019 YvesCheung
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


  [1]: https://raw.githubusercontent.com/YvesCheung/SlidableLayout/master/material/slidableLayout.gif
  [2]: https://github.com/YvesCheung/SlidableLayout/raw/master/material/SlidableLayoutHorizontal.gif
  [3]: https://raw.githubusercontent.com/YvesCheung/SlidableLayout/master/material/NestedScroll.gif
  [4]: https://raw.githubusercontent.com/YvesCheung/SlidableLayout/master/material/OppositeNestedScroll.gif
  [5]: https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout
  [6]: https://developer.android.com/reference/android/support/v4/view/NestedScrollingChild
  [7]: https://github.com/YvesCheung/SlidableLayout/tree/master/app/src/main/java/com/yy/mobile/slidablelayout
