package com.slicehouse.pos.data

/** Prices are in sen: 2000 = RM20.00. */
object SampleData {

    /** 10" = RM20, 11" = RM25, 12" = RM30 for the standard pizzas. */
    val sizes = listOf(
        SizeOption("s10", "10\"", "Small", 0),
        SizeOption("s11", "11\"", "Medium", 500),
        SizeOption("s12", "12\"", "Large", 1_000)
    )

    val toppings = listOf(
        Topping("cheese", "Extra cheese", "🧀", 300)
    )

    val menu = listOf(
        pizza("p1", "4in1", "Four flavours in one wood-fired pizza", "🍕", "pizza_4in1", twelveInch = true),
        pizza("p2", "Aloha", "Chicken ham, pineapple and mozzarella", "🍍", "pizza_aloha"),
        pizza("p3", "BBQ", "BBQ sauce, chicken and red onion", "🍗", "pizza_bbq"),
        pizza("p4", "Beef Pepperoni", "Beef pepperoni, mozzarella and oregano", "🥩", "pizza_beef_pepperoni", twelveInch = true),
        pizza("p5", "Chicken Pepperoni", "Chicken pepperoni, mozzarella and oregano", "🍗", "pizza_chicken_pepperoni", twelveInch = true),
        pizza("p6", "Daebak", "Korean spicy chicken, mozzarella and scallion", "🌶️", "pizza_daebak"),
        pizza("p7", "Prawn", "Prawn, garlic butter and mozzarella", "🦐", "pizza_prawn"),
        pizza("p8", "Sardine", "Sardine, onion and chili", "🐟", "pizza_sardine"),
        pizza("p9", "Seafood", "Prawn, squid, crab stick and mozzarella", "🦐", "pizza_seafood"),
        pizza("p10", "Smoke Beef", "Smoked beef, mozzarella and onion", "🥩", "pizza_smoke_beef", twelveInch = true, twelveInchDelta = 2_000),
        pizza("p11", "Smoke Duck", "Smoked duck, mozzarella and spring onion", "🦆", "pizza_smoke_duck", twelveInch = true, twelveInchDelta = 2_000),
        pizza("p12", "Super Supreme", "Beef, chicken, pepperoni and vegetables", "🍕", "pizza_super_supreme"),
        pizza("p13", "Tuna", "Tuna, onion and mozzarella", "🐟", "pizza_tuna"),

        MenuItem("s1", "Garlic Bread", "Baked with garlic butter and parsley", "🥖", Category.SIDES, 450),
        MenuItem("s2", "Cheesy Garlic Bread", "Mozzarella, garlic butter, oregano", "🧄", Category.SIDES, 550),
        MenuItem("s3", "Chicken Wings", "Six wings, hot honey glaze", "🍗", Category.SIDES, 750),
        MenuItem("s4", "Mozzarella Sticks", "Six sticks, marinara dip", "🧀", Category.SIDES, 600),
        MenuItem("s5", "Caesar Salad", "Romaine, parmesan, croutons", "🥗", Category.SIDES, 650),
        MenuItem("s6", "Fries", "Sea salt, rosemary", "🍟", Category.SIDES, 400),

        MenuItem("d1", "Coca-Cola", "330 ml can", "🥤", Category.DRINKS, 250),
        MenuItem("d2", "Sparkling Water", "500 ml bottle", "💧", Category.DRINKS, 200),
        MenuItem("d3", "Lemonade", "Fresh squeezed", "🍋", Category.DRINKS, 300),
        MenuItem("d4", "Iced Tea", "Peach, no sugar", "🧊", Category.DRINKS, 275),
        MenuItem("d5", "Espresso", "Single shot", "☕", Category.DRINKS, 250),
        MenuItem("d6", "Birra Moretti", "330 ml bottle", "🍺", Category.DRINKS, 450),

        MenuItem("x1", "Tiramisu", "Mascarpone, espresso, cocoa", "🍰", Category.DESSERTS, 550),
        MenuItem("x2", "Nutella Pizza", "Wood-fired dough, Nutella, hazelnut", "🍫", Category.DESSERTS, 700),
        MenuItem("x3", "Gelato", "Two scoops, pistachio or vanilla", "🍨", Category.DESSERTS, 400),
        MenuItem("x4", "Cannoli", "Ricotta, candied orange", "🥐", Category.DESSERTS, 450)
    )

    /** Every pizza starts at RM20 (10"); 11" adds RM5 and 12" adds RM10. */
    private fun pizza(
        id: String,
        name: String,
        description: String,
        emoji: String,
        imageKey: String,
        twelveInch: Boolean = false,
        twelveInchDelta: Sen = 1_000
    ) = MenuItem(
        id = id,
        name = name,
        description = description,
        emoji = emoji,
        category = Category.PIZZA,
        price = 2_000,
        available = true,
        isPizza = true,
        imageKey = imageKey,
        twelveInchAvailable = twelveInch,
        twelveInchDelta = twelveInchDelta
    )
}
