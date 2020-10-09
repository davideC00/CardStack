package io.github.davidec00.cardstack

import androidx.compose.animation.AnimatedFloatModel
import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableConstants
import androidx.compose.material.ThresholdConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import kotlin.math.abs
import kotlin.math.sign

/**
 * Controller of the [draggableStack] modifier.
 *
 * @param clock The animation clock that will be used to drive the animations.
 * @param screenWidth The width of the screen used to calculate properties such as rotation and scale
 * @param animationSpec The default animation that will be used to animate swipes.
 */
open class CardStackController(
    clock: AnimationClockObservable,
    private val screenWidth: Float,
    internal val animationSpec: AnimationSpec<Float> = SwipeableConstants.DefaultAnimationSpec
) {
    /**
     * Whether the state is currently animating.
     */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        private set

    /**
     * Anchors
     */
    val right = Offset(screenWidth, 0f)
    val left = Offset(-screenWidth, 0f)
    val center = Offset(0f, 0f)

    /**
     * Threshold to start swiping
     */
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

    /**
     * The current position (in pixels) of the First Card.
     */
    val offsetX = AnimatedFloatModel(0f, animationClockProxy)
    val offsetY = AnimatedFloatModel(0f, animationClockProxy)

    /**
     * The current rotation (in pixels) of the First Card.
     */
    val rotation = AnimatedFloatModel(0f, animationClockProxy)

    /**
     * The current scale factor (in pixels) of the Card before the first one displayed.
     */
    val scale = AnimatedFloatModel(0.8f, animationClockProxy)

    var onSwipeLeft : () -> Unit = {}
    var onSwipeRight: () -> Unit = {}


    fun swipeLeft(){
        offsetX.animateTo(-screenWidth, animationSpec) { endReason, endOffset ->
            onSwipeLeft()
            // After the animation of swiping return back to Center to make it look like a cycle
            offsetX.snapTo(center.x)
            offsetY.snapTo(0f)
            rotation.snapTo(0f)
            scale.snapTo(0.8f)
        }
        scale.animateTo(1f, animationSpec)
    }

    fun swipeRight(){
        offsetX.animateTo(screenWidth, animationSpec) { endReason, endOffset ->
            onSwipeRight()
            // After the animation return back to Center to make it look like a cycle
            offsetX.snapTo(center.x)
            offsetY.snapTo(0f)
            rotation.snapTo(0f)
            scale.snapTo(0.8f)
        }
        scale.animateTo(1f, animationSpec)
    }

    fun returnCenter(){
        offsetX.animateTo(center.x, animationSpec)
        offsetY.animateTo(center.y, animationSpec)
        rotation.animateTo(0f, animationSpec)
        scale.animateTo(0.8f, animationSpec)
    }

}

/**
 * Create and [remember] a [CardStackController] with the default animation clock.
 *
 * @param animationSpec The default animation that will be used to animate to a new state.
 */
@Composable
fun rememberCardStackController(
        animationSpec: AnimationSpec<Float> = SwipeableConstants.DefaultAnimationSpec,
): CardStackController {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val screenWidth = with(DensityAmbient.current){
        ConfigurationAmbient.current.screenWidthDp.dp.toPx()
    }
    return remember {
        CardStackController(
                clock = clock,
                screenWidth = screenWidth,
                animationSpec = animationSpec
        )
    }
}

/**
 * Enable drag gestures between a set of predefined anchors defined in [controller].
 *
 * @param controller The controller of the [draggableStack].
 * @param thresholdConfig Specifies where the threshold between the predefined Anchors is. This is represented as a lambda
 * that takes two float and returns the threshold between them in the form of a [ThresholdConfig].
 * @param velocityThreshold The threshold (in dp per second) that the end velocity has to exceed
 * in order to swipe, even if the positional [thresholds] have not been reached.
 */
@ExperimentalMaterialApi
fun Modifier.draggableStack(
    controller: CardStackController,
    thresholdConfig: (Float, Float) -> ThresholdConfig,
    velocityThreshold: Dp = 125.dp
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
                    super.onStop(velocity)
                    if(controller.offsetX.value <= 0f){
                        if (velocity.x <= -velocityThresholdPx) {
                            controller.swipeLeft()
                        } else {
                            if (controller.offsetX.value > -controller.threshold) controller.returnCenter()
                            else controller.swipeLeft()
                        }
                    }else{
                        if (velocity.x >= velocityThresholdPx) {
                            controller.swipeRight()
                        } else {
                            if (controller.offsetX.value < controller.threshold) controller.returnCenter()
                            else controller.swipeRight()
                        }
                    }
                }

                override fun onDrag(dragDistance: Offset): Offset {
                    if(!controller.isAnimationRunning){
                        controller.offsetX.snapTo(controller.offsetX.value + dragDistance.x)
                        controller.offsetY.snapTo(controller.offsetY.value + dragDistance.y)
                        val targetRotation = normalize(controller.center.x, controller.right.x, abs(controller.offsetX.value), 0f, 10f)
                        controller.rotation.snapTo(targetRotation * -controller.offsetX.value.sign)
                        controller.scale.snapTo(normalize(controller.center.x, controller.right.x/3, abs(controller.offsetX.value), 0.8f))
                    }
                    return super.onDrag(dragDistance)
                }
            },
            canStartDragging = {!controller.isAnimationRunning}
    )

    draggable.onPositioned { }
}

/**
 * Min max normalization
 *
 * @param min Minimum of the range
 * @param max Maximum of the range
 * @param v Value to normalize in the given [min, max] range
 * @param startRange Transform the normalized value with a particular start range
 * @param endRange Transform the normalized value with a particular end range
 */
fun normalize (min: Float, max: Float , v: Float, startRange: Float = 0f, endRange: Float = 1f): Float{
    require(startRange < endRange){
        "Start range is greater than End range"
    }
    val value = v.coerceIn(min, max)
    return ( value - min )/( max - min )*(endRange - startRange ) + startRange
}
