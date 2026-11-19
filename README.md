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
- **Staff PINs** – each worker signs in with their own 4–6 digit PIN, and orders record who took
  them. Refunds, menu and price changes, shop settings and staff management need the owner PIN
  (a cashier gets an "Owner approval" pad). Cashiers can still mark items sold out.
- **Adaptive layout** – on a phone the cart slides up as a bottom sheet with bottom navigation; on a
  tablet you get a navigation rail and a permanent cart panel.
- **Offline-first register** – orders, the menu and settings live in a local Room database. FireLord
  and Firebase sync are optional add-ons used only when the owner connects them.
- **Customer ordering web app (`/web`)** – a mobile-first React + Vite + Tailwind + Firebase web app
  where customers browse the live menu, customise pizzas, place pickup orders, and track status in
  real time at `/order/:orderId`.
- **Firebase Cloud Firestore** – the Android tablet listens to `online_orders` in real time, moves
  orders through the same kitchen workflow, saves paid web orders locally as `#W...`, and publishes
  menu/price/sold-out changes back to the web. All money is stored in whole sen (`Long`) to match
  `Money.kt`.

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

## Staff PINs

1. On first launch (also after upgrading from v1.5 or earlier) create the **owner account**: name and
   a 4–6 digit PIN.
2. The app then shows a 12-character **recovery code** once. Write it down and keep it away from the
   tablet: it's the only way to reset a forgotten owner PIN (**Forgot owner PIN?** on the lock screen).
3. Add cashiers in **Settings → Staff → Manage staff**.
4. Tap **Lock** (side rail or bottom bar) to hand over the tablet. The app doesn't lock by itself.

PINs are stored only as salted PBKDF2 hashes. After 5 wrong PINs, entry is blocked for 30 seconds.

## Connect to FireLord (optional)

The tablet can copy every order to the shop owner's FireLord site and take its menu from there.
Without connecting, the app works exactly as before, offline and on its own.

1. On FireLord open **Pizza POS → Tablets → Get a pairing code**. The 6-digit code works once, for
   10 minutes.
2. On the tablet (signed in as owner) open **Settings → FireLord → Connect to FireLord**, type the
   FireLord address and the code.
3. The first sync sends the tablet's menu up if FireLord has none yet; otherwise FireLord's menu
   replaces the tablet's. Every order already on the tablet is uploaded.

While connected:

- Each sale and refund is sent right away, and everything is checked again every minute while the
  app is open. Without internet, orders wait on the tablet and go up later. **Settings → FireLord**
  shows how many are waiting.
- FireLord holds the master menu. Adding or editing items, prices and toppings needs the internet;
  marking an item **sold out** works offline and is sent later. Changes made on FireLord arrive on
  the next sync.
- The tax rate is set on FireLord. **Reset all data** needs disconnecting first.
- If the owner disconnects the tablet on FireLord, it stops syncing and says so.

### Online orders

Customers can order from the shop's link or QR code (FireLord → Pizza POS → Online → Hours, slots
and QR code) and pay at pickup. On a connected tablet the **Online** tab shows those orders, with a
badge and a beep when a new one is waiting:

1. **Accept** (or **Reject**) the order. Nothing is cooked until the shop accepts it.
2. **Start preparing** → **Into the oven** → **Ready for pickup**. The customer's tracking page
   follows each step.
3. When the customer pays: **Paid cash** or **Paid card**. The order is then saved on the tablet as
   `#W12` (W = web) and counts in the day's orders and totals.

Orders can be cancelled until they go in the oven, and a ready order can be marked **Not
collected**. The same buttons are on FireLord's web board, so either can be used.

Release builds only connect over `https://`. Debug builds also allow `http://`, e.g.
`http://10.0.2.2:8000` to reach a FireLord running on the PC from the emulator.

## Firebase customer web app (new)

Pizza Kayu Api Dungun's customer ordering site lives in `web/`. It is a React + Vite + Tailwind
app, ready for Firebase Hosting, and shares the exact menu and money rules with the tablet:

- 13 pizzas, 6 sides, 6 drinks and 4 desserts from `SampleData.kt`, with the 13 bundled pizza
  photos copied from `app/src/main/res/drawable-nodpi` into `web/public/images`.
- Sizes 10" (RM20), 11" (RM25), 12" (RM30; RM40 for Smoke Beef/Smoke Duck, and only for 4in1,
  Beef Pepperoni, Chicken Pepperoni, Smoke Beef and Smoke Duck), Extra cheese RM3, item notes,
  quantities, tax in basis points, and totals in whole sen.
- Firestore `onSnapshot` listeners for live menu prices/availability, and `/order/:orderId`
  tracking: Pending → Accepted → Preparing → In Oven → Ready for Pickup → Completed.
- A web order appears on the tablet's **Online** tab immediately (badge + beep). Accept, Reject,
  Start preparing, Into the oven, Ready for pickup, Paid cash/card and Cancel/Not collected all
  write back to the same Firestore document. Paid orders are saved to the Room DB as `#W...` and
  sent to the configured Bluetooth kitchen/receipt printer.

### Firestore schema

All amounts are whole sen (`Long`), not floats. Timestamps are Firestore Timestamps.

| Collection / doc | Fields | Writes |
| --- | --- | --- |
| `menus/{itemId}` | `id, name, description, emoji, imageKey, category, priceSen, available, isPizza, twelveInchAvailable, twelveInchDeltaSen, sortIndex, updatedAt` | Admin only |
| `toppings/{toppingId}` | `id, label, emoji, priceSen` | Admin only |
| `settings/general` | `shopName, currency, phone, taxBasisPoints, ...` | Admin only |
| `counters/online_orders` | `next` (Int) | Narrow public increment |
| `online_orders/{orderId}` | `orderNumber, label, status, createdAt, updatedAt, orderType, customerUid, customerName, customerPhone, note, pickupTime, subtotalSen, taxSen, totalSen, taxBasisPoints, payment, cancelReason, statusHistory, items[]` | Public create, admin status updates |

`items[]` mirrors the POS `CartLine` (including display-label toppings, `sizeLabel` and per-item
`note`). `firestore.rules` validates `customerName`, `customerPhone`, `items` and `totalSen` on
creation, keeps the customer's identity/items/total immutable afterwards, and only lets an admin
move status. Customers sign in anonymously (no password) so they can read only their own order.

### Set up Firebase

1. Create a Firebase project, add a **Web app** and an **Android app** with package
   `com.slicehouse.pos`. Enable **Cloud Firestore**, **Authentication → Anonymous** and
   **Authentication → Email/Password**.
2. Replace `your-firebase-project-id` in `.firebaserc` with your project id.
3. Copy `web/.env.example` to `web/.env.local` and fill in the `VITE_FIREBASE_*` values from the
   web app's config. Do not commit `.env.local`.
4. Copy `app/google-services.example.json` to `app/google-services.json` and replace all
   placeholders with the Android app's real config. This file is gitignored; the Gradle plugin
   is applied only when it exists, so a fresh clone still builds without it.
5. Deploy the rules and indexes: `firebase deploy --only firestore` (or
   `firebase emulators:start --only firestore` for local testing).
6. Seed the menu. Either install a service account key in `web/service-account.json` and run
   `cd web && npm run seed`, or open the site's `/admin` page and use the seed button after
   signing in with an admin account. Menu edits and sold-out switches made on the tablet also
   publish automatically.
7. Give your owner account the admin claim (needed for status updates, menu writes and to see
   the live order list on the tablet):

   ```bash
   cd web
   GOOGLE_APPLICATION_CREDENTIALS=./service-account.json npm run admin:claim -- owner@example.com
   ```

   Create that email/password user in Firebase Authentication first. On the tablet open
   **Settings → Firebase web orders** and sign in with it, then sign out and back in once more
   if the claim was added while already signed in.
8. Build and deploy the site: `cd web && npm run build`, then `firebase deploy --only hosting`.
   `firebase.json` publishes `web/dist` with SPA rewrites so `/order/:id` works on refresh.

The legacy FireLord REST sync can stay connected at the same time: the Online tab merges both
sources, and Firestore orders take precedence on id collisions. Without any Firebase config the
Android app still builds and works exactly as before; the web app shows the sample menu but
disables checkout.

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

Requirements: JDK 17 and Android SDK Platform 35 + Build-Tools 35. The Firebase plugin is
applied only when `app/google-services.json` exists, so the build also works without Firebase
credentials.

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
    MenuMaster.kt          Where menu edits go while connected to FireLord
    db/                    Room database: tables, queries and converters
    sync/                  FireLord REST client (OkHttp), JSON shapes, SyncManager and
                           FirebaseSyncManager (Firestore web orders + menu publishing)
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
    SettingsScreen.kt      Shop configuration, Firebase admin sign-in, theme and printer
    FirebaseViewModel.kt   Firebase admin sign-in state
    Components.kt          Shared cards, receipt and formatting
    theme/Theme.kt         Light and dark colour schemes

web/                       Customer ordering app (React + Vite + Tailwind + Firebase Web SDK)
  src/data/menu.js         Menu copied from SampleData.kt, photos and size rules
  src/lib/                 Whole-sen money helpers and cart/status logic
  src/components/          Menu, native-dialog customizer, cart, checkout, tracker, admin seed
  public/images/           The 13 bundled pizza photos
  scripts/                 Firestore seed and admin-claim helpers (Admin SDK)
firebase.json              Firebase Hosting (web/dist) + Firestore rules/indexes
firestore.rules            Public order creation, private tracking, admin-only writes
.firebaserc                Replace the placeholder with your Firebase project id
```

## Data & privacy

The app requests Bluetooth permissions for the receipt printer, and internet access for the
optional FireLord / Firebase sync. Orders, the menu and settings live in a Room (SQLite)
database, `pizza_pos.db`, inside the app's private storage. Each save is all-or-nothing, so a
crash or power cut can't leave half an order behind. Money is stored as whole sen, so totals
never drift. Web orders in Cloud Firestore contain the customer's name, phone, items and notes
and are readable only by that customer (anonymous Auth UID) or an admin; do not put card numbers
in order notes. Uninstalling the app removes local data, so back up the device if it matters.

Upgrading from v1.4: on first launch the app imports the old `slicehouse_pos.json` and renames it to
`slicehouse_pos.imported.json` (it is never deleted). If the old file can't be read, it is left
untouched and the app shows a message instead of silently starting empty.

## Default menu

The 13 pizzas from the shop's photo set ship with the app: 4in1, Aloha, BBQ, Beef Pepperoni,
Chicken Pepperoni, Daebak, Prawn, Sardine, Seafood, Smoke Beef, Smoke Duck, Super Supreme and Tuna,
each with its photo. Six sides, six drinks and four desserts are also included, along with the extra
topping options (Extra cheese, RM3, plus custom toppings) and three sizes (10", 11", 12"). The starting
prices are placeholders — change them in **Menu** to match your real price list.
