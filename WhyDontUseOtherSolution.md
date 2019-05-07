# 为什么我不用ViewPager或RecyclerView来做上下滑切换

上下滑切换翻页大概是这样的效果：

![SlidableLayout][1]

目前网上有诸多如 “仿抖音上下滑...” “仿花椒映客直播...”  之类的技术分享，都有讲述如何实现一个上下滑切换页面的方案，其中就有 `ViewPager` 和 `RecyclerView` + `SnapHelper` 两种主流的方案，但是都有明显的缺点。更优的解决方案待会再说，以下是一些个人的看法：

## 为什么ViewPager不合适

`ViewPager` 自带的滑动效果完全满足场景，而且支持 `Fragment` 和 `View` 等UI绑定，只要对布局和触摸事件部分作一些修改，就可以把横向的 `ViewPager` 改成竖向。

但是**没有复用是个最致命的问题**。在 `onLayout` 方法中，所有子View会实例化并一字排开在布局上。当Item数量很大时，将会是很大的性能浪费。

其次是**可见性判断的问题**。很多人会以为 `Fragment` 在 `onResume` 的时候就是可见的，而 `ViewPager` 中的 `Fragment` 就是个反例，尤其是多个 `ViewPager` 嵌套时，会同时有多个父 `Fragment` 多个子 `Fragment` 处于 `onResume` 的状态，却只有其中一个是可见的。除非放弃 `ViewPager` 的预加载机制。在页面内容曝光等重要的数据上报时，就需要判断很多条件：`onResumed` 、 `setUserVisibleHint` 、 `setOnPageChangeListener` 等。

最后是**嵌套滑动的问题**。同向嵌套滑动是很常见的场景，Google 新出的滑动布局基本都使用 NestedScrolling 机制来解决嵌套滑动。但是 ViewPager 依然需要开发者自己来处理复杂的滑动冲突。

## 为什么RecyclerView不合适

`RecyclerView` + `SnapHelper` 的方案比 `ViewPager` 好得多，既有对 `View` 的复用，滑动事件也已经处理好。
但是依然**无法双向无限**滑动。我们可以在 `getItemCount` 方法中返回 Integer.MAX_VALUE 来假装无限。但是为了从头开始就可以下拉滑到上一个，索引就不能初始化为0，那初始值为 Integer.MAX_VALUE/2 ?
无论怎么掩饰，理论上还是有滑动到头的一天。

## 更优的一种解决方案

**使用两个 View 轮流切换就能完成上下滑的场景**。这种方案也有APP在用，但是网上几乎找不到源码。因此我把它抽成独立的库放在Github仓库：[致力于打造通用、易用和流畅的上下滑动翻页布局SlidableLayout][2]。

[SlidableLayout][2] 本质是一个包含两个相同大小子 `View` 的 `FrameLayout` 。两个子 `View` 分别作为 **TopView** 和 **BackView** 。

静止状态下，用户只会看见 **TopView** ，而 **BackView** 被移除或隐藏。

手指向上拖动时， **TopView** 在y轴上向上偏移， **BackView** 开始出现，而且 **BackView** 的顶部与 **TopView** 的底部相接。

手指向上拖动一定距离后放手，**TopView** 继续在y轴上做动画直到完全消失， **BackView** 向上直到完全出现。然后 **TopView** 和 **BackView** 互换身份，原来的 **BackView** 成为现在的 **TopView** ，原来的 **TopView** 被移除或隐藏，成为下一次滑动的 **BackView** 。互换后完成一次滑动。

反之，手指向下滑动亦然。

同时要考虑手指放手后，滑动距离不够或者速度不够时，**TopView** 会沿着y轴回弹到原来的位置。 **BackView** 也跟着原路返回，直到被移除或隐藏。

[SlidableLayout][2] 还实现了 NestedScrollingChild 接口，使其能够与自定义的下拉刷新布局嵌套滑动。

源码和使用例子参照 [https://github.com/YvesCheung/SlidableLayout][2] 。如有不同意的地方，请在 Github 留下 **Issue**。



  [1]: https://raw.githubusercontent.com/YvesCheung/SlidableLayout/master/material/slidableLayout.gif
  [2]: https://github.com/YvesCheung/SlidableLayout
