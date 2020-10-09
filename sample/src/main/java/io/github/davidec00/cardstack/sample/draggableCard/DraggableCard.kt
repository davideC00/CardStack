package io.github.davidec00.cardstack.sample.draggableCard

import androidx.compose.animation.AnimatedFloatModel
import androidx.compose.animation.asDisposableClock
import androidx.compose.foundation.Box
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.ThresholdConfig
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawShadow
import androidx.compose.ui.drawLayer
import androidx.compose.ui.layout
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.ConfigurationAmbient
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.dp
import io.github.davidec00.cardstack.*
import io.github.davidec00.cardstack.draggableCard.draggableCard
import io.github.davidec00.cardstack.draggableCard.rememberDraggableCardController
import kotlin.math.abs


@ExperimentalMaterialApi
@Composable
fun CardStack2(modifier : Modifier = Modifier, items: MutableList<io.github.davidec00.cardstack.Item>){
    var i by remember { mutableStateOf(items.size-1) }
    val scale = AnimatedFloatModel(1f, AnimationClockAmbient.current.asDisposableClock())
    val screenWidth = with(DensityAmbient.current){
        ConfigurationAmbient.current.screenWidthDp.dp.toPx()
    }

    Stack(modifier = modifier) {

        items.asReversed().forEachIndexed { index, item ->
            DraggableCard(modifier = modifier
                    .drawLayer(
                            scaleX = if(index < i) scale.value else 1f,
                            scaleY = if(index < i) scale.value else 1f)
                    .visibility(index <= i),
                    item = item,
                    thresholdConfig = { _, _ -> FractionalThreshold(0.2f) },
                    onSwipeLeft = {
                        i--
                        scale.animateTo(1f)
                    },
                    onSwipeRight = {
                        i--
                        scale.animateTo(1f)
                    },
                    onDrag = {x, y ->
                        scale.snapTo(
                            normalize(
                                0f,
                                screenWidth / 6,
                                abs(x),
                                0.8f
                            )
                        )
                    })
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun DraggableCard(
    modifier: Modifier,
    item: io.github.davidec00.cardstack.Item,
    thresholdConfig: (Float, Float) -> ThresholdConfig,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onDrag: (x: Float, y : Float) -> Unit = {_, _ ->}
){
    val cardStackState = rememberDraggableCardController()

    Box(modifier = Modifier
            .draggableCard(
                    controller = cardStackState,
                    thresholdConfig = thresholdConfig,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    onDrag = onDrag)
    ){
        io.github.davidec00.cardstack.Card(
            modifier = modifier
                .moveTo(
                    x = cardStackState.offsetX.value,
                    y = cardStackState.offsetY.value
                )
                .drawLayer(
                    rotationZ = cardStackState.rotation.value
                )
                .drawShadow(2.dp, RoundedCornerShape(10.dp))
                .fillMaxSize(),
            item
        )
    }
}

fun Modifier.visibility(value: Boolean = true) = this.then(Modifier.layout{ measurable, constraints ->
    val placeable = measurable.measure(constraints)
    if(value){
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0,0)
        }
    }else{
        layout(0, 0) {}
    }

})

