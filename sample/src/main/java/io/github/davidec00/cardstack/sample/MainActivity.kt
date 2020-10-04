package io.github.davidec00.cardstack.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import io.github.davidec00.cardstack.CardStack
import io.github.davidec00.cardstack.Item

class MainActivity : AppCompatActivity(){
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        val item1 = Item(
            "https://f4.bcbits.com/img/0020592180_10.jpg",
            "Jane",
            "16 miles near you"
        )
        val item2 = Item(
            "https://images.pexels.com/photos/91224/pexels-photo-91224.jpeg?auto=compress&cs=tinysrgb&dpr=3&h=750&w=1260",
            "Robert",
            "7 miles near you"
        )
        val item3 = Item(
            "https://images.pexels.com/photos/789812/pexels-photo-789812.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=650&w=940",
            "Daria",
            "3 miles from you"
        )
        val item4 = Item(
            "https://images.pexels.com/photos/872756/pexels-photo-872756.jpeg?cs=srgb&dl=pexels-dishan-lathiya-872756.jpg&fm=jpg",
            "Violet",
            "43 miles from you"
        )
        val items = mutableListOf(item1, item2, item3, item4)
        super.onCreate(savedInstanceState)
        //val imagesIds = mutableListOf(R.drawable.pic1, R.drawable.pic2)

        setContent {
            CardStack(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                enableButtons = true,
                items = items
            )
        }
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



