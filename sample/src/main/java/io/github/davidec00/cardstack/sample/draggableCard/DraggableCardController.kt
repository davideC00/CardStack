package io.github.davidec00.cardstack.draggableCard

import androidx.compose.animation.AnimatedFloatModel
import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableConstants
import androidx.compose.material.ThresholdConfig
import androidx.compose.material.swipeable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.rawDragGestureFilter
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.ConfigurationAmbient
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


open class DraggableCardController(
        clock: AnimationClockObservable,
        private val screenWidth: Float,
        internal val animationSpec: AnimationSpec<Float> = SwipeableConstants.DefaultAnimationSpec
) {
    /**
     * Whether the state is currently animating.
     */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        private set

    val right = Offset(screenWidth, 0f)
    val left = Offset(-screenWidth, 0f)
    val center = Offset(0f, 0f)
    var threshold: Float = 0.0f

    // May Change
    private val animationClockProxy: AnimationClockObservable = object : AnimationClockObservable {
        override fun subscribe(observer: AnimationClockObserver) {
            isAnimationRunning = true
            clock.subscribe(observer)
        }

        override fun unsubscribe(observer: AnimationClockObserver) {
            isAnimationRunning = false
            clock.unsubscribe(observer)
        }
    }

    internal var thresholds: (Float, Float) -> Float by mutableStateOf({ _, _ -> 0f })


    val rotation = AnimatedFloatModel(0f, animationClockProxy)
    /**
     * The current position (in pixels) of the [swipeable]. Used in offsetPx of content
     */
    val offsetX = AnimatedFloatModel(0f, animationClockProxy)
    val offsetY = AnimatedFloatModel(0f, animationClockProxy)

    fun swipeLeft( onSwipe: ()->Unit = {}){
        // Left swipe
        offsetX.animateTo(left.x, animationSpec) { endReason, endOffset ->
            // After the animation return back to 0f to make it look like a cycle
            onSwipe()
        }
        offsetY.animateTo(center.y, animationSpec)
        rotation.animateTo(0f, animationSpec)

    }

    fun swipeRight(){
        offsetX.animateTo(right.x, animationSpec) { endReason, endOffset ->
            // After the animation return back to Center to make it look like a cycle
        }
        offsetY.animateTo(right.y, animationSpec)
        rotation.animateTo(0f, animationSpec)


    }

    fun returnCenter(){
        offsetX.animateTo(center.x, animationSpec)
        offsetY.animateTo(center.y, animationSpec)
        rotation.animateTo(0f, animationSpec)
    }

}


@Composable
fun rememberDraggableCardController(
        animationSpec: AnimationSpec<Float> = SwipeableConstants.DefaultAnimationSpec,
): DraggableCardController {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val screenWidth = with(DensityAmbient.current){
        ConfigurationAmbient.current.screenWidthDp.dp.toPx()
    }
    return remember {
        DraggableCardController(
                clock = clock,
                screenWidth = screenWidth,
                animationSpec = animationSpec
        )
    }
}


@ExperimentalMaterialApi
fun Modifier.draggableCard(
        controller: DraggableCardController,
        thresholdConfig: (Float, Float) -> ThresholdConfig,
        velocityThreshold: Dp = 125.dp,
        onSwipeLeft: () -> Unit = {},
        onSwipeRight: () -> Unit = {},
        onDrag: (x: Float, y: Float) -> Unit = { _, _ -> Unit }
) = composed {
    val density = DensityAmbient.current
    val velocityThresholdPx = with(density) { velocityThreshold.toPx() }
    val thresholds = { a: Float, b: Float ->
        with(thresholdConfig(a,b)){
            density.computeThreshold(a,b)
        }
    }
    controller.threshold = thresholds(controller.center.x, controller.right.x)
    val draggable = Modifier.rawDragGestureFilter(
            object: DragObserver {
                override fun onStop(velocity: Offset) {
                    // it handles when the user stop the drag
                    super.onStop(velocity)

                    if(controller.offsetX.value <= 0f){
                        if (velocity.x <= -velocityThresholdPx) {
                            controller.swipeLeft()
                        } else {
                            if (controller.offsetX.value > -controller.threshold) controller.returnCenter()
                            else {
                                controller.swipeLeft(){
                                    onSwipeLeft()
                                }

                            }
                        }
                    }else{
                        if (velocity.x >= velocityThresholdPx) {
                            controller.swipeRight()
                        } else {
                            if (controller.offsetX.value < controller.threshold) controller.returnCenter()
                            else {
                                controller.swipeRight()
                                onSwipeRight()
                            }
                        }
                    }

                }

                override fun onDrag(dragDistance: Offset): Offset {
                    controller.offsetX.snapTo(controller.offsetX.value + dragDistance.x)
                    controller.offsetY.snapTo(controller.offsetY.value + dragDistance.y)
                    controller.rotation.snapTo(-controller.offsetX.value/30)
                    return super.onDrag(dragDistance)
                }
            },
            canStartDragging = {!controller.isAnimationRunning}
    )

    draggable.onPositioned { }
}


