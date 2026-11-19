/**
 * A verbatim copy of the Android app's SampleData.kt, used as the offline fallback
 * and as the source for the Firestore seed. Money is always whole sen (Long on the POS).
 * Keys and image names match `menuImageRes()` in ui/MenuImages.kt.
 */

export const CATEGORIES = [
  { id: 'PIZZA', label: 'Pizzas', emoji: '🍕' },
  { id: 'SIDES', label: 'Sides', emoji: '🍟' },
  { id: 'DRINKS', label: 'Drinks', emoji: '🥤' },
  { id: 'DESSERTS', label: 'Desserts', emoji: '🍰' },
]

export const SIZE_OPTIONS = [
  { id: 's10', label: '10"', detail: 'Small', deltaSen: 0 },
  { id: 's11', label: '11"', detail: 'Medium', deltaSen: 500 },
  { id: 's12', label: '12"', detail: 'Large', deltaSen: 1_000 },
]

export const SEED_TOPPINGS = [{ id: 'cheese', label: 'Extra cheese', emoji: '🧀', priceSen: 300 }]

export const SEED_SETTINGS = {
  shopName: 'Pizza Kayu Api Dungun',
  location: 'Alur Tempoyak, Dungun, Terengganu',
  currency: 'RM',
  phone: '601114927780',
  taxBasisPoints: 800, // 800 = 8%, same basis points as Settings.taxBasisPoints in the POS.
  instagram: 'https://www.instagram.com/pizza.alorpoyok',
  tiktok: '@nazalinaalias',
  locationUrl: 'https://maps.app.goo.gl/wVPagLXnoqLt8s279',
}

const pizza = (id, name, description, emoji, imageKey, twelveInchAvailable = false, twelveInchDeltaSen = 1_000, sortIndex = 0) => ({
  id,
  name,
  description,
  emoji,
  imageKey,
  category: 'PIZZA',
  priceSen: 2_000,
  available: true,
  isPizza: true,
  twelveInchAvailable,
  twelveInchDeltaSen,
  sortIndex,
})

const simple = (id, name, description, emoji, category, priceSen, sortIndex) => ({
  id,
  name,
  description,
  emoji,
  imageKey: '',
  category,
  priceSen,
  available: true,
  isPizza: false,
  twelveInchAvailable: false,
  twelveInchDeltaSen: 1_000,
  sortIndex,
})

export const SEED_MENU = [
  pizza('p1', '4in1', 'Four flavours in one wood-fired pizza', '🍕', 'pizza_4in1', true, 1_000, 0),
  pizza('p2', 'Aloha', 'Chicken ham, pineapple and mozzarella', '🍍', 'pizza_aloha', false, 1_000, 1),
  pizza('p3', 'BBQ', 'BBQ sauce, chicken and red onion', '🍗', 'pizza_bbq', false, 1_000, 2),
  pizza('p4', 'Beef Pepperoni', 'Beef pepperoni, mozzarella and oregano', '🥩', 'pizza_beef_pepperoni', true, 1_000, 3),
  pizza('p5', 'Chicken Pepperoni', 'Chicken pepperoni, mozzarella and oregano', '🍗', 'pizza_chicken_pepperoni', true, 1_000, 4),
  pizza('p6', 'Daebak', 'Korean spicy chicken, mozzarella and scallion', '🌶️', 'pizza_daebak', false, 1_000, 5),
  pizza('p7', 'Prawn', 'Prawn, garlic butter and mozzarella', '🦐', 'pizza_prawn', false, 1_000, 6),
  pizza('p8', 'Sardine', 'Sardine, onion and chili', '🐟', 'pizza_sardine', false, 1_000, 7),
  pizza('p9', 'Seafood', 'Prawn, squid, crab stick and mozzarella', '🦐', 'pizza_seafood', false, 1_000, 8),
  pizza('p10', 'Smoke Beef', 'Smoked beef, mozzarella and onion', '🥩', 'pizza_smoke_beef', true, 2_000, 9),
  pizza('p11', 'Smoke Duck', 'Smoked duck, mozzarella and spring onion', '🦆', 'pizza_smoke_duck', true, 2_000, 10),
  pizza('p12', 'Super Supreme', 'Beef, chicken, pepperoni and vegetables', '🍕', 'pizza_super_supreme', false, 1_000, 11),
  pizza('p13', 'Tuna', 'Tuna, onion and mozzarella', '🐟', 'pizza_tuna', false, 1_000, 12),

  simple('s1', 'Garlic Bread', 'Baked with garlic butter and parsley', '🥖', 'SIDES', 450, 20),
  simple('s2', 'Cheesy Garlic Bread', 'Mozzarella, garlic butter, oregano', '🧄', 'SIDES', 550, 21),
  simple('s3', 'Chicken Wings', 'Six wings, hot honey glaze', '🍗', 'SIDES', 750, 22),
  simple('s4', 'Mozzarella Sticks', 'Six sticks, marinara dip', '🧀', 'SIDES', 600, 23),
  simple('s5', 'Caesar Salad', 'Romaine, parmesan, croutons', '🥗', 'SIDES', 650, 24),
  simple('s6', 'Fries', 'Sea salt, rosemary', '🍟', 'SIDES', 400, 25),

  simple('d1', 'Coca-Cola', '330 ml can', '🥤', 'DRINKS', 250, 30),
  simple('d2', 'Sparkling Water', '500 ml bottle', '💧', 'DRINKS', 200, 31),
  simple('d3', 'Lemonade', 'Fresh squeezed', '🍋', 'DRINKS', 300, 32),
  simple('d4', 'Iced Tea', 'Peach, no sugar', '🧊', 'DRINKS', 275, 33),
  simple('d5', 'Espresso', 'Single shot', '☕', 'DRINKS', 250, 34),
  simple('d6', 'Birra Moretti', '330 ml bottle', '🍺', 'DRINKS', 450, 35),

  simple('x1', 'Tiramisu', 'Mascarpone, espresso, cocoa', '🍰', 'DESSERTS', 550, 40),
  simple('x2', 'Nutella Pizza', 'Wood-fired dough, Nutella, hazelnut', '🍫', 'DESSERTS', 700, 41),
  simple('x3', 'Gelato', 'Two scoops, pistachio or vanilla', '🍨', 'DESSERTS', 400, 42),
  simple('x4', 'Cannoli', 'Ricotta, candied orange', '🥐', 'DESSERTS', 450, 43),
]

/** Sizes shown for a menu item. Only the five 12"-capable pizzas get the large size. */
export function sizesFor(item) {
  if (!item?.isPizza) return []
  return item.twelveInchAvailable ? SIZE_OPTIONS : SIZE_OPTIONS.filter((size) => size.id !== 's12')
}

/** Mirrors PizzaDialog.kt: the per-item 12" delta wins over the catalog's size delta. */
export function sizeDeltaSen(item, sizeId) {
  if (!item?.isPizza) return 0
  if (sizeId === 's12') return item.twelveInchDeltaSen
  return SIZE_OPTIONS.find((size) => size.id === sizeId)?.deltaSen ?? 0
}

export function imageUrl(item) {
  return item?.imageKey ? `/images/${item.imageKey}.jpg` : ''
}
