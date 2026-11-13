# Pizza Kayu Api Dungun POS

A complete, offline point-of-sale for Pizza Kayu Api Dungun, built as a native Android app with
Kotlin and Jetpack Compose.

## What's inside

- **Register** – category tabs, live menu search, a pizza builder (size, extra toppings including a
  free-form custom topping, and a kitchen note), a running cart, order types (dine-in / takeaway /
  delivery), pickup time, table numbers, customer name and phone number, delivery address and
  automatic tax.
- **Checkout** – cash or card, quick cash buttons, change due, and a guard against taking too little
  cash. The order label shows the pickup time when set.
- **Receipts** – an on-screen receipt after payment, a shareable plain-text receipt, direct
  printing to a Bluetooth thermal printer, plus a simple kitchen ticket that can be printed before
  payment (no social links or footer, includes the pickup time).
- **Bluetooth printing** – pick a paired ESC/POS printer (58 mm or 80 mm) in Settings, then print a
  test receipt or print any receipt from the payment dialog or order history.
- **Orders** – today's sales, order count and average ticket, full order history, refunds and one-tap
  reorder.
- **Menu manager** – add, edit, delete and hide menu items, pick a bundled photo for any item, plus edit
  the extra toppings (Extra cheese, RM3, or any custom topping) and their prices.
- **Settings** – shop name, address, phone, Instagram, TikTok, Google Maps link, tax rate, currency
  symbol, receipt footer, light/dark/system theme and the Bluetooth printer. Reset data when you want
  a clean slate.
- **Adaptive layout** – on a phone the cart slides up as a bottom sheet with bottom navigation; on a
  tablet you get a navigation rail and a permanent cart panel.
- **Offline first** – no internet permission. Everything is stored in a local database on the device.

Pizza sizes are 10" (RM20), 11" (RM25) and 12" (RM30). Only 4in1, Beef Pepperoni, Chicken Pepperoni,
Smoke Beef and Smoke Duck come in 12", with Smoke Beef and Smoke Duck at RM40. Extra cheese is RM3.

The app icon is generated from `Logo.png`, and the shop defaults are pre-filled with:

- Phone: `601114927780`
- Instagram: `https://www.instagram.com/pizza.alorpoyok`
- TikTok: `@nazalinaalias`
- Maps: `https://maps.app.goo.gl/wVPagLXnoqLt8s279`

## What's new in v1.4

- Custom pizza toppings: the register's extra-toppings picker now also has a free-form **Custom
  topping** option, so any topping not on the list can be added at any price.
- Pickup time: take orders (takeaway / delivery) with a **Pickup time** on the register, and the time
  is printed on the kitchen ticket, the full receipt, the shareable text receipt and the order detail.
- Menu manager: pizza **price and 12 inch size/price** can now be edited per item (base price plus the
  12 inch availability and 12 inch price).
- Receipts: the pizza line now prints as **pizza + size/toppings + price on one line** instead of
  splitting it into separate lines.

## Install on a phone or tablet

1. Copy `PizzaKayuApiDungunPOS.apk` (a copy of the signed release build at
   `app/build/outputs/apk/release/app-release.apk`) to the device (USB, cloud drive, etc.).
2. Tap the file and allow "install unknown apps" for the app you opened it with.
3. Or install over USB with `adb install -r PizzaKayuApiDungunPOS.apk`.

Requires Android 8.0 (API 26) or newer. Works on phones and tablets in portrait or landscape.

## Bluetooth printer setup

1. Pair the printer in **Android Settings → Bluetooth** first (default PIN is usually `0000` or
   `1234`).
2. Open the app's **Settings → Bluetooth printer → Choose printer** and select it. On Android 12+
   allow the Bluetooth permission prompt.
3. Choose **58 mm** or **80 mm** paper, then tap **Test print**.
4. Receipts can then be printed from the payment receipt dialog or from **Orders → order → Print**.

The app speaks ESC/POS over Bluetooth SPP, which covers the common Xprinter, Goojprt, Epson and
compatible thermal printers. It does not use the internet or any cloud service.

## Build from source

Requirements: JDK 17 and Android SDK Platform 35 + Build-Tools 35.

```bash
# Windows
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

The release build is signed with the bundled development keystore
(`keystore/pizza-pos.jks`, store password `pizzapos`, key alias `pizza-pos`, key password
`pizzapos`). **Replace it with your own keystore before shipping to a store.**

## Project layout

```
app/src/main/java/com/slicehouse/pos/
  MainActivity.kt          Single activity, edge-to-edge Compose host
  PosApplication.kt        Owns the AppContainer for the life of the app
  AppContainer.kt          Builds the store once and hands it to ViewModels
  data/
    Models.kt              Menu, cart, order, settings and theme models
    Money.kt               Money in whole sen (Long), tax and price parsing
    SampleData.kt          Seed menu, sizes, crusts and toppings
    PosStore.kt            App state; saves every change to the database in order
    LegacyImport.kt        One-time import of the v1.4 JSON file
    db/                    Room database: tables, queries and converters
  print/
    BluetoothPrinter.kt    Bluetooth SPP connection to the thermal printer
    ReceiptPrinter.kt      ESC/POS receipt builder for 58/80 mm paper
  ui/
    *ViewModel.kt          One per screen: its state and actions (screen logic lives here, not in the UI)
    PosViewModelFactory.kt Creates the ViewModels from the AppContainer
    App.kt                 Navigation rail / bottom bar + system bars
    RegisterScreen.kt      Menu browser + cart
    PizzaDialog.kt         Pizza builder
    CheckoutScreen.kt      Payment
    ReceiptDialog.kt       Post-payment receipt + print/share
    PrintControls.kt       Shared Bluetooth print button
    OrdersScreen.kt        Sales stats + order history
    MenuScreen.kt          Menu and topping management
    MenuImages.kt          Bundled pizza photos and photo picker options
    SettingsScreen.kt      Shop configuration, theme and printer
    Components.kt          Shared cards, receipt and formatting
    theme/Theme.kt         Light and dark colour schemes
```

## Data & privacy

The app requests Bluetooth permissions for the receipt printer and no internet permission. Orders,
the menu and settings live in a Room (SQLite) database, `pizza_pos.db`, inside the app's private
storage. Each save is all-or-nothing, so a crash or power cut can't leave half an order behind.
Money is stored as whole sen, so totals never drift. Uninstalling the app removes the data, so back
up the device if it matters.

Upgrading from v1.4: on first launch the app imports the old `slicehouse_pos.json` and renames it to
`slicehouse_pos.imported.json` (it is never deleted). If the old file can't be read, it is left
untouched and the app shows a message instead of silently starting empty.

## Default menu

The 13 pizzas from the shop's photo set ship with the app: 4in1, Aloha, BBQ, Beef Pepperoni,
Chicken Pepperoni, Daebak, Prawn, Sardine, Seafood, Smoke Beef, Smoke Duck, Super Supreme and Tuna,
each with its photo. Six sides, six drinks and four desserts are also included, along with the extra
topping options (Extra cheese, RM3, plus custom toppings) and three sizes (10", 11", 12"). The starting
prices are placeholders — change them in **Menu** to match your real price list.
