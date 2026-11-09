package com.slicehouse.pos.ui

import androidx.annotation.DrawableRes
import com.slicehouse.pos.R

/** A bundled menu photo that can be assigned to a menu item. */
data class MenuPhotoOption(val key: String, val label: String)

/** All photos that ship with the app, in menu order. */
val menuPhotoOptions: List<MenuPhotoOption> = listOf(
    MenuPhotoOption("pizza_4in1", "4in1"),
    MenuPhotoOption("pizza_aloha", "Aloha"),
    MenuPhotoOption("pizza_bbq", "BBQ"),
    MenuPhotoOption("pizza_beef_pepperoni", "Beef Pepperoni"),
    MenuPhotoOption("pizza_chicken_pepperoni", "Chicken Pepperoni"),
    MenuPhotoOption("pizza_daebak", "Daebak"),
    MenuPhotoOption("pizza_prawn", "Prawn"),
    MenuPhotoOption("pizza_sardine", "Sardine"),
    MenuPhotoOption("pizza_seafood", "Seafood"),
    MenuPhotoOption("pizza_smoke_beef", "Smoke Beef"),
    MenuPhotoOption("pizza_smoke_duck", "Smoke Duck"),
    MenuPhotoOption("pizza_super_supreme", "Super Supreme"),
    MenuPhotoOption("pizza_tuna", "Tuna")
)

/** Resolves a stored [com.slicehouse.pos.data.MenuItem.imageKey] to a drawable, or null. */
@DrawableRes
fun menuImageRes(key: String): Int? = when (key) {
    "pizza_4in1" -> R.drawable.pizza_4in1
    "pizza_aloha" -> R.drawable.pizza_aloha
    "pizza_bbq" -> R.drawable.pizza_bbq
    "pizza_beef_pepperoni" -> R.drawable.pizza_beef_pepperoni
    "pizza_chicken_pepperoni" -> R.drawable.pizza_chicken_pepperoni
    "pizza_daebak" -> R.drawable.pizza_daebak
    "pizza_prawn" -> R.drawable.pizza_prawn
    "pizza_sardine" -> R.drawable.pizza_sardine
    "pizza_seafood" -> R.drawable.pizza_seafood
    "pizza_smoke_beef" -> R.drawable.pizza_smoke_beef
    "pizza_smoke_duck" -> R.drawable.pizza_smoke_duck
    "pizza_super_supreme" -> R.drawable.pizza_super_supreme
    "pizza_tuna" -> R.drawable.pizza_tuna
    else -> null
}
