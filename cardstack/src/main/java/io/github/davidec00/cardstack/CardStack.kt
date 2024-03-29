package io.github.davidec00.cardstack

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDownAlt
import androidx.compose.material.icons.filled.ThumbUpAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/**
 * A stack of cards that can be dragged.
 * If they are dragged after a [thresholdConfig] or exceed the [velocityThreshold] the card is swiped.
 *
 * @param items Cards to show in the stack.
 * @param thresholdConfig Specifies where the threshold between the predefined Anchors is. This is represented as a lambda
 * that takes two float and returns the threshold between them in the form of a [ThresholdConfig].
 * @param velocityThreshold The threshold (in dp per second) that the end velocity has to exceed
 * in order to swipe, even if the positional [thresholds] have not been reached.
 * @param enableButtons Show or not the buttons to swipe or not
 * @param onSwipeLeft Lambda that executes when the animation of swiping left is finished
 * @param onSwipeRight Lambda that executes when the animation of swiping right is finished
 * @param onEmptyRight Lambda that executes when the cards are all swiped
 */
@ExperimentalMaterialApi
@Composable
fun CardStack(modifier : Modifier = Modifier,
              items: MutableList<Item>,
              thresholdConfig: (Float, Float) -> ThresholdConfig = { _, _ -> FractionalThreshold(0.2f) },
              velocityThreshold: Dp = 125.dp,
              enableButtons: Boolean = false,
              onSwipeLeft : ( item : Item) -> Unit = {},
              onSwipeRight : ( item : Item) ->  Unit = {},
              onEmptyStack : ( lastItem : Item) -> Unit = {}
){

    var i by remember { mutableStateOf(items.size-1)}

    if( i == -1 ){
        onEmptyStack( items.last() )
    }

    val cardStackController = rememberCardStackController()
    cardStackController.onSwipeLeft = {
        onSwipeLeft(items[i])
        i--
    }
    cardStackController.onSwipeRight = {
        onSwipeRight(items[i])
        i--
    }

    ConstraintLayout(modifier = modifier.fillMaxSize().padding(20.dp)) {
        val (buttons, stack) = createRefs()

        if(enableButtons){
            Row( modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(buttons){
                        bottom.linkTo(parent.bottom)
                        top.linkTo(stack.bottom)
                    },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
            ){
                FloatingActionButton(
                        onClick = { if (i >= 0) cardStackController.swipeLeft() },
                        backgroundColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(5.dp)
                ) {
                    Icon(Icons.Filled.ThumbDownAlt, contentDescription = "", tint = Color.Red)
                }
                Spacer( modifier = Modifier.width(70.dp))
                FloatingActionButton(
                        onClick = { if (i >= 0) cardStackController.swipeRight() },
                        backgroundColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(5.dp)
                ) {
                    Icon(Icons.Filled.ThumbUpAlt,contentDescription = "", tint = Color.Green)
                }
            }
        }

        Box(modifier = Modifier
                .constrainAs(stack){
                    top.linkTo(parent.top)
                }
                .draggableStack(
                        controller = cardStackController,
                        thresholdConfig = thresholdConfig,
                        velocityThreshold = velocityThreshold
                )
                .fillMaxHeight(0.8f)
        ){
            items.asReversed().forEachIndexed{ index, item ->
                Card(modifier = Modifier
                        .moveTo(
                                x = if (index == i) cardStackController.offsetX.value else 0f,
                                y = if (index == i) cardStackController.offsetY.value else 0f
                        )
                        .visible( visible = index == i || index == i - 1)
                        .graphicsLayer(
                                rotationZ = if (index == i) cardStackController.rotation.value else 0f,
                                scaleX = if (index < i) cardStackController.scale.value else 1f,
                                scaleY = if (index < i) cardStackController.scale.value else 1f
                        )
                        .shadow(4.dp, RoundedCornerShape(10.dp)),
                        item
                )
            }
        }
    }
}

@Composable
fun Card(
        modifier: Modifier = Modifier,
        item: Item = Item()
){
    Box(
            modifier
    ){
        if(item.url != null){
            AsyncImage(
                model = item.url,
                contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
            )
        }
        Column(modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ){
            Text(text = item.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    modifier = Modifier.clickable(onClick = {}) // disable the highlight of the text when dragging
            )
            Text(text = item.subText,
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable(onClick = {}) // disable the highlight of the text when dragging
            )
        }
    }
}

data class Item(
        val url: String? = null,
        val text: String = "",
        val subText: String = ""
)

fun Modifier.moveTo(
    x: Float,
    y: Float
) = this.then(Modifier.layout{measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height){
        placeable.placeRelative(x.roundToInt(),y.roundToInt())
    }
})

fun Modifier.visible(
        visible: Boolean = true
) = this.then(Modifier.layout{measurable, constraints ->
    val placeable = measurable.measure(constraints)
    if(visible){
        layout(placeable.width, placeable.height){
            placeable.placeRelative(0,0)
        }
    }else{
        layout(0, 0) {}
    }
})

