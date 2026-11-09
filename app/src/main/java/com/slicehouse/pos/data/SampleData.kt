package com.slicehouse.pos.data

object SampleData {

    /** 10" = RM20, 11" = RM25, 12" = RM30 for the standard pizzas. */
    val sizes = listOf(
        SizeOption("s10", "10\"", "Small", 0.0),
        SizeOption("s11", "11\"", "Medium", 5.0),
        SizeOption("s12", "12\"", "Large", 10.0)
    )

    val toppings = listOf(
        Topping("cheese", "Extra cheese", "\uD83E\uDDC0", 3.00)
    )

    val menu = listOf(
        pizza("p1", "4in1", "Four flavours in one wood-fired pizza", "\uD83C\uDF55", "pizza_4in1", twelveInch = true),
        pizza("p2", "Aloha", "Chicken ham, pineapple and mozzarella", "\uD83C\uDF4D", "pizza_aloha"),
        pizza("p3", "BBQ", "BBQ sauce, chicken and red onion", "\uD83C\uDF57", "pizza_bbq"),
        pizza("p4", "Beef Pepperoni", "Beef pepperoni, mozzarella and oregano", "\uD83E\uDD69", "pizza_beef_pepperoni", twelveInch = true),
        pizza("p5", "Chicken Pepperoni", "Chicken pepperoni, mozzarella and oregano", "\uD83C\uDF57", "pizza_chicken_pepperoni", twelveInch = true),
        pizza("p6", "Daebak", "Korean spicy chicken, mozzarella and scallion", "\uD83C\uDF36\uFE0F", "pizza_daebak"),
        pizza("p7", "Prawn", "Prawn, garlic butter and mozzarella", "\uD83E\uDD90", "pizza_prawn"),
        pizza("p8", "Sardine", "Sardine, onion and chili", "\uD83D\uDC1F", "pizza_sardine"),
        pizza("p9", "Seafood", "Prawn, squid, crab stick and mozzarella", "\uD83E\uDD90", "pizza_seafood"),
        pizza("p10", "Smoke Beef", "Smoked beef, mozzarella and onion", "\uD83E\uDD69", "pizza_smoke_beef", twelveInch = true, twelveInchDelta = 20.0),
        pizza("p11", "Smoke Duck", "Smoked duck, mozzarella and spring onion", "\uD83E\uDD86", "pizza_smoke_duck", twelveInch = true, twelveInchDelta = 20.0),
        pizza("p12", "Super Supreme", "Beef, chicken, pepperoni and vegetables", "\uD83C\uDF55", "pizza_super_supreme"),
        pizza("p13", "Tuna", "Tuna, onion and mozzarella", "\uD83D\uDC1F", "pizza_tuna"),

        MenuItem("s1", "Garlic Bread", "Baked with garlic butter and parsley", "\uD83E\uDD56", Category.SIDES, 4.50),
        MenuItem("s2", "Cheesy Garlic Bread", "Mozzarella, garlic butter, oregano", "\uD83E\uDDC4", Category.SIDES, 5.50),
        MenuItem("s3", "Chicken Wings", "Six wings, hot honey glaze", "\uD83C\uDF57", Category.SIDES, 7.50),
        MenuItem("s4", "Mozzarella Sticks", "Six sticks, marinara dip", "\uD83E\uDDC0", Category.SIDES, 6.00),
        MenuItem("s5", "Caesar Salad", "Romaine, parmesan, croutons", "\uD83E\uDD57", Category.SIDES, 6.50),
        MenuItem("s6", "Fries", "Sea salt, rosemary", "\uD83C\uDF5F", Category.SIDES, 4.00),

        MenuItem("d1", "Coca-Cola", "330 ml can", "\uD83E\uDD64", Category.DRINKS, 2.50),
        MenuItem("d2", "Sparkling Water", "500 ml bottle", "\uD83D\uDCA7", Category.DRINKS, 2.00),
        MenuItem("d3", "Lemonade", "Fresh squeezed", "\uD83C\uDF4B", Category.DRINKS, 3.00),
        MenuItem("d4", "Iced Tea", "Peach, no sugar", "\uD83E\uDDCA", Category.DRINKS, 2.75),
        MenuItem("d5", "Espresso", "Single shot", "\u2615", Category.DRINKS, 2.50),
        MenuItem("d6", "Birra Moretti", "330 ml bottle", "\uD83C\uDF7A", Category.DRINKS, 4.50),

        MenuItem("x1", "Tiramisu", "Mascarpone, espresso, cocoa", "\uD83C\uDF70", Category.DESSERTS, 5.50),
        MenuItem("x2", "Nutella Pizza", "Wood-fired dough, Nutella, hazelnut", "\uD83C\uDF6B", Category.DESSERTS, 7.00),
        MenuItem("x3", "Gelato", "Two scoops, pistachio or vanilla", "\uD83C\uDF68", Category.DESSERTS, 4.00),
        MenuItem("x4", "Cannoli", "Ricotta, candied orange", "\uD83E\uDD50", Category.DESSERTS, 4.50)
    )

    /** Every pizza starts at RM20 (10"); 11" adds RM5 and 12" adds RM10. */
    private fun pizza(
        id: String,
        name: String,
        description: String,
        emoji: String,
        imageKey: String,
        twelveInch: Boolean = false,
        twelveInchDelta: Double = 10.0
    ) = MenuItem(
        id = id,
        name = name,
        description = description,
        emoji = emoji,
        category = Category.PIZZA,
        price = 20.00,
        available = true,
        isPizza = true,
        imageKey = imageKey,
        twelveInchAvailable = twelveInch,
        twelveInchDelta = twelveInchDelta
    )
}
