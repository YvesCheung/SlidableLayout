# SlidableLayout

[![License](https://img.shields.io/badge/License%20-Apache%202-337ab7.svg)](https://www.apache.org/licenses/LICENSE-2.0)  [![](https://jitpack.io/v/YvesCheung/SlidableLayout.svg)](https://jitpack.io/#YvesCheung/SlidableLayout)

**SlidableLayout** 致力于打造通用、易用和流畅的上下滑动翻页布局。专注于通用的上下切换场景，包括但不限于直播间切换、阅读图书翻页、短视频应用等。

## 效果预览

![SlidableLayout][1]

## 特性
- 通用的基本场景，可以上下滑切换 `View` 或者 `Fragment` 
- 使用适配器模式，继承 `SlideAdapter` 、 `SlideViewAdapter` 或者  `SlideFragmentAdapter` 来自定义业务逻辑
- 只复用两个 `View` ( `Fragment` )， 上下滑只是轮流切换两个 `View` 的位置，没有多余的性能消耗
- 充足的时序回调，可以在滑动过程中掌握 *开始可见* ，*完全可见*，*完全不可见* 的时机
- 支持无限滑动
- 支持嵌套滑动，可与其他实现 `NestedScrolling` 机制的布局配合使用，比如 [SwipeRefreshLayout][2] 等刷新加载布局

## 与其他方案的对比
目前网络上大部分上下滑方案都是围绕 `ViewPager` 或者 `RecyclerView` + `SnapHelper` 。
这是我的个人见解：[为什么我不用ViewPager或RecyclerView来做上下滑布局][3]。

## 使用

### 在XML或者代码中添加SlidableLayout
```xml
<com.yy.mobile.widget.SlidableLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

`SlidableLayout` 本身实现了 [NestedScrollingChild][4] 接口，因此可以在外层嵌套其他滑动布局，比如自定义你的下拉刷新与上拉加载。

### 适配器业务逻辑

以滑动切换 `Fragment` 为例子，先自定义继承 `SlideFragmentAdapter` 并实现对 UI 的绑定，以及是否可以滑动的判断：

```kotlin
class MyAdapter(fm: FragmentManager) : SlideFragmentAdapter(fm) {

    private val data = listOf("a", "b", "c", "d")
    private var currentIndex = 0

    /**
     * 能否向 [direction] 的方向滑动。
     *
     * @param direction 滑动的方向
     * @return 返回 true 表示可以滑动， false 表示不可滑动。
     * 如果有嵌套其他外层滑动布局（比如下拉刷新），当且仅当返回 false 时会触发外层的嵌套滑动。
     */
    override fun canSlideTo(direction: SlideDirection): Boolean {
        val index =
            when (direction) {
                SlideDirection.Next -> currentIndex + 1 //能否滑向下一个
                SlideDirection.Prev -> currentIndex - 1 //能否滑向上一个
                else -> currentIndex
            }
        return index in 0 until data.size
    }

    /**
     * 创建要显示的 [Fragment]。
     * 一般来说，该方法会在 [SlidableLayout.setAdapter] 调用时触发一次，创建当前显示的 [Fragment]，
     * 会在首次开始滑动时触发第二次，创建滑动目标的 [Fragment]。
     */
    override fun onCreateFragment(context: Context): Fragment {
        return DemoFragment()
    }

    /**
     * 把 [direction] 方向那个数据与 [fragment] 绑定。做一些 ui 的显示操作。
     */
    override fun onBindFragment(fragment: Fragment, direction: SlideDirection) {
        val index =
            when (direction) {
                SlideDirection.Next -> currentIndex + 1 //绑定下一个的数据
                SlideDirection.Prev -> currentIndex - 1 //绑定上一个的数据
                SlideDirection.Origin -> currentIndex
            }
        //bind data to the ui
        (fragment as DemoFragment).currentData = data[index]
    }

    /**
     * 滑动结束后回调
     */
    override fun finishSlide(direction: SlideDirection) {
        super.finishSlide(direction)
        // 修正当前的索引
        currentIndex =
            when (direction) {
                SlideDirection.Next -> currentIndex + 1 //已经滑向下一个
                SlideDirection.Prev -> currentIndex - 1 //已经滑向上一个
                SlideDirection.Origin -> currentIndex //原地回弹
            }
    }
}
```

通过 `setAdapter` 方法就会把 `Fragment` 显示到 `SlideableLayout` 上：

```kotlin
slidable_layout.setAdapter(MyAdapter(supportFragmentManager))
```

更详细的适配器使用可以参照 [demo][5] 。

### 滑动时机回调

在 `SlideAdapter` 中，通过 `onCreateView` 或者 `onCreateFragment` 创建滑动切换的 `View` 或者 `Fragment` 。这些自定义的 `View` 或者 `Fragment` 可以实现 `SlidableUI` 接口，来监听滑动的时机回调：

```kotlin
class DemoFragment : Fragment(), SlidableUI {

    override fun startVisible(direction: SlideDirection) {
        // 滑动开始，当前视图将要可见
        // 可以在该回调中实现数据与视图的绑定，比如显示占位的图片
    }

    override fun completeVisible(direction: SlideDirection) {
        // 滑动完成，当前视图完全可见
        // 可以在该回调中开始主业务，比如开始播放视频
    }

    override fun invisible(direction: SlideDirection) {
        // 滑动完成，当前视图完全不可见
        // 可以在该回调中做一些清理工作，比如关闭播放器
    }

    override fun preload(direction: SlideDirection) {
        // 已经完成了一次 direction 方向的滑动，用户很可能会在这个方向上继续滑动
        // 可以在该回调中实现下一次滑动的预加载，比如开始下载下一个视频或者准备好封面图
    }
}
```

## 安装
1. 根目录的 build.gradle 中添加
    ```groovy
    allprojects {
    	repositories {
    		...
    		maven { url 'https://jitpack.io' }
    	}
    }
    ```

2. 对应要使用的模块中添加依赖
    ```groovy
    dependencies {
        // Support library
        // 如果使用的是Support包，添加以下依赖
        implementation 'com.github.YvesCheung:SlidableLayout:1.0.4'
        //implementation "com.android.support:support-fragment:$support_version"
        
        // AndroidX
        // 如果使用的是AndroidX，添加以下依赖
        implementation 'com.github.YvesCheung:SlidableLayout:1.0.4.x'
        //implementation "androidx.fragment:fragment:1.0.0"
    }
    ```


## 许可证

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
  [2]: https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout
  [3]: https://github.com/YvesCheung/SlidableLayout/blob/master/WhyDontIUseOtherSolution.md
  [4]: https://developer.android.com/reference/android/support/v4/view/NestedScrollingChild
  [5]: https://github.com/YvesCheung/SlidableLayout/tree/master/app/src/main/java/com/yy/mobile/slidablelayout
