package it.fast4x.riplay.extensions.games.pacman.models

import android.util.Log
import it.fast4x.riplay.extensions.games.pacman.utils.GameConstants
import kotlin.random.Random


data class PacFood(
    val foodList: ArrayList<PacFoodModel> = ArrayList(),
    val bonusFoodList: ArrayList<PacFoodModel> = ArrayList() // bonus food which reverses the enemy path back to their box
) {
    init {
        initPacFoodList()
        initBonusPacFoodList()
    }

    private fun initPacFoodList() {
        foodList.clear()



        for (i in 0 until GameConstants.FOOD_COUNTER) {
            val food = PacFoodModel(
                xPos = Random.nextInt(85, 850),
                yPos = Random.nextInt(85, 1200),
                size = 0.5f
            )
            Log.w("food", "${food.xPos}")
            foodList.add(food)

        }
    }

    private fun initBonusPacFoodList() {
        bonusFoodList.clear()

        // topLeft
        bonusFoodList.add(
            PacFoodModel(
                xPos = 90,
                yPos = 85,
                size = 1f
            )
        )

        // top right
        bonusFoodList.add(PacFoodModel(
            xPos = 825,
            yPos = 85,
            size = 1f
        ))

        // bottom left
        bonusFoodList.add(PacFoodModel(
            xPos = 90,
            yPos = 1150,
            size = 1f
        ))

        // bottom Right
        bonusFoodList.add(PacFoodModel(
            xPos = 825,
            yPos = 1150,
            size = 1f
        ))


    }

    fun initRedraw() {
        initPacFoodList()
        initBonusPacFoodList()
    }
}


data class PacFoodModel(
    var xPos: Int,
    var yPos: Int,
    var size: Float
)