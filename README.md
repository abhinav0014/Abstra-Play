# 📺 StreamSphere — Android App

A beautiful, lightweight Android TV channel browser built with **Kotlin + Jetpack Compose + Material Design 3**.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🌐 **Live API Data** | Pulls channels, streams & logos from iptv-org.github.io |
| 🇳🇵 **Nepal Tab** | All Nepali channels (NTV, Kantipur, Himalaya, etc.) |
| 🇮🇳 **India Tab** | All Indian channels (DD National, Star, Zee, Sony…) |
| 🔬 **Science Tab** | Science, Education & Kids channels worldwide |
| 🎵 **Music Tab** | Music & Entertainment channels worldwide |
| ❤️ **Favourites** | Save channels locally with Room DB |
| 📱 **Widget** | Add up to 4 channels to home screen via Glance API |
| 🔍 **Search** | Real-time search across name & country |
| 🎨 **Material You** | Dynamic dark/light theme, M3 components |
| ⚡ **Lightweight** | In-memory cache + lazy loading — no lag |

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── api/         → Retrofit (IptvApi) + Room (AppDatabase, FavouritesDao)
│   ├── model/       → Channel, Stream, Logo, FavouriteChannel, ChannelUiModel
│   └── repository/  → ChannelRepository (single source of truth)
├── viewmodel/       → ChannelViewModel (Hilt, StateFlow)
├── ui/
│   ├── screens/     → Home, Search, Favourites, Detail, Settings
│   ├── components/  → ChannelCard, CategoryTabRow, LogoImage, LiveBadge…
│   ├── theme/       → Material3 Theme, Colors, Typography
│   └── navigation/  → Bottom nav + NavHost
└── widget/          → GlanceAppWidget (ChannelWidget + ChannelWidgetReceiver)
```

**Stack:** Kotlin · Jetpack Compose · Hilt DI · Retrofit + OkHttp · Room · Coil · Glance Widgets · Splash Screen API

---

## 🚀 Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35

### Steps

1. **Clone / open** the project in Android Studio
2. **Sync Gradle** — all dependencies download automatically
3. **Run** on a device or emulator (API 24+)

> **Internet permission** is declared in `AndroidManifest.xml`. Make sure the device has network access.

---

## 📱 Screens

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Home Screen   │  │  Discover/Search │  │   Favourites    │  │    Settings     │
│                 │  │                 │  │                 │  │                 │
│ StreamSphere    │  │ 🔍 Search…      │  │ ❤️ 3 saved      │  │ 📺 StreamSphere │
│ Global TV       │  │                 │  │                 │  │ v1.0            │
│                 │  │ [Channel Card]  │  │ 📱 Widget (2)   │  │                 │
│ [Search bar]   │  │ [Channel Card]  │  │ ───────────     │  │ 🌙 Dark Theme   │
│                 │  │ [Channel Card]  │  │ ⭐ All (3)      │  │ 🔔 Alerts       │
│ 🌐 🇳🇵 🇮🇳 🔬 🎵 │  │     …           │  │ [Channel Card]  │  │ 📱 Widget Setup │
│                 │  │                 │  │ [Channel Card]  │  │ ℹ️  About        │
│ [Channel Card]  │  │                 │  │ [Channel Card]  │  │                 │
│ [Channel Card]  │  │                 │  │                 │  │                 │
│ [Channel Card]  │  │                 │  │                 │  │                 │
└────┬────────────┘  └────┬────────────┘  └────┬────────────┘  └────┬────────────┘
     │ 🏠 Home  🔍 Discover  ❤️ Faves  ⚙️ Settings │
     └──────────────────────────────────────────────┘
                     Bottom Navigation Bar
```

---

## 🧩 Home Screen Widget

Long-press your home screen → **Widgets** → **StreamSphere Channel Widget**

The widget displays up to **4 favourite channels** with live indicators. Tap to open the app.

To add channels to the widget:
1. Heart ❤️ a channel (favourite it)
2. Tap the **📱 widget icon** on the card, or go to **Detail screen → "Add to Widget"**

---

## 🎨 Design Decisions

- **Dark-first** design: `#080C14` background, deep navy surfaces
- **Color coding**: Nepal=Red, India=Orange, Science=Blue, Music=Purple
- **Staggered grid**: Adaptive columns that fill screen width intelligently  
- **Lazy loading**: Channels load progressively; staggered animation delays (30ms/item, max 300ms)
- **In-memory cache**: API data cached in repository after first fetch — no repeated network calls
- **Pull-to-refresh**: Swipe down on Home screen to reload channels
- **Animated transitions**: Page-level slide + fade; card entrance animations; bouncy FAV button

---

## 📦 Key Dependencies

```toml
compose-bom          = "2024.06.00"
navigation-compose   = "2.7.7"
hilt                 = "2.51.1"
retrofit             = "2.11.0"
room                 = "2.6.1"
coil                 = "2.6.0"
glance-appwidget     = "1.1.0"
core-splashscreen    = "1.0.1"
```

---

## 📝 Proguard

The `release` build type enables `isMinifyEnabled = true` and `isShrinkResources = true` for a lean APK. Rules in `proguard-rules.pro` preserve Retrofit/Gson models and Room entities.

---

## 🤝 Data Source

Channel data powered by the open-source **[iptv-org](https://github.com/iptv-org/iptv)** project.
API: `https://iptv-org.github.io/api/`
