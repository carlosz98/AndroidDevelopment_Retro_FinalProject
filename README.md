# 🕹️ RetroHub — Retro Gaming Social Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

RetroHub is an Android application built with Kotlin and Jetpack Compose, designed as a community platform for retro gaming enthusiasts. It features a **Scrapbook/Comic aesthetic** — cream backgrounds, bold black borders, yellow accents, and Bangers font throughout — serving as a hub where users can discover, explore, and engage with retro content: classic gaming magazines, soundtracks, community articles, live streams, a game database, events calendar, and marketplace.

Powered by Firebase (Auth, Firestore, Storage — Blaze plan), IGDB for game data, Twitch for live streams, and YouTube Data API v3.

---

## 🌟 Features

### 🎨 UI & Design System
- Custom **Scrapbook/Comic** theme — `ScrapbookCream`, `ScrapbookYellow`, `ScrapbookDark`, offset shadow cards
- `BangersFontFamily` for all headers, `NunitoFontFamily` for body text
- `ScrapbookCard()` — reusable white card with black border and offset shadow
- Animated screen transitions (scale + fade) between all sections
- Side drawer navigation with animated talking robot mascot and Retro Radio player
- Bottom navigation: Home, Discover, Messages, Profile

### 🏠 Home Screen
- **Hero section** — full-width image with gradient overlay, RETROHUB title, EXPLORE NOW CTA
- **Today in Retro Gaming** — dark card with rotating historical facts and date
- **Community strip** — live user avatars from Firestore with FIND MORE PEOPLE button
- **Explore RetroHub** — 2×2 nav grid with taller cards (160dp), play button overlays, gradient
- **Featured Albums + Magazines** carousels with SEE ALL navigation
- **Did You Know?** rotating retro facts with refresh button
- **Random Game Picker** — animated slot machine spin with 30+ classic games
- **Streams & Videos** — scrapbook-style card with Twitch + YouTube buttons
- **Latest News** — RSS feed with article images, source badge, date
- Retro Quote of the Moment — rotating famous game quotes

### 👤 Profile
- Firebase Auth — email/password + Google Sign-In
- **5-step profile setup:** username availability check, banner + profile photo upload, top 6 games (IGDB), top 3 soundtracks, gaming platforms
- Full profile: banner, circular avatar, XP progress bar, level badge
- **Gaming personality radar chart** — animated spider chart auto-detected from top games (RPG/Action/Platformer/Shooter/Adventure/Arcade)
- **Activity heatmap** — 7×7 grid showing last 7 weeks of activity with streak stats
- **Pinned article** — users pin one of their own articles to their profile
- **Stream bubbles** — clickable Twitch (purple) + YouTube (red) channel buttons
- Platform bubbles: PlayStation, Xbox, Steam, Nintendo
- Followers/Following lists, follow/unfollow, tap to view other profiles
- Edit profile: all fields including Twitch + YouTube usernames
- Recent Activity feed with article + bookmark + follow events
- XP + achievement badges system
- Favorites tab (bookmarked content)

### 🔍 Discover
- **Filter strip** — ALL / PEOPLE / ARTICLES / MAGAZINES / ALBUMS / LIVE pill chips
- **Featured Article hero card** — most viewed article with gradient overlay
- **Trending Players** — top 3 users by followers, stacked card with gold/silver/bronze badges
- **Live Now section** — community streamers currently live on Twitch
- **Horizontal carousels** — articles, magazines, albums in LazyRow with SEE ALL
- Real-time Firestore user search by username or handle
- Content search across magazines, albums, articles

### 💬 Messages (Chat)
- Real-time DMs and group chats via Firestore
- Emoji reactions on messages
- Image sharing with blur + ⚠️ reveal safety
- Unread badge on bottom nav
- New chat screen — search users, select multiple for group, set group name
- Chat list with last message preview and timestamps

### 📰 Magazines
- **3 tabs:** BROWSE / SHELVES / CHALLENGES
- Virtual magazine shelf with 4-per-row scrollable covers
- **Continue Reading strip** — resume from where you left off (Firestore progress)
- **My Shelves** — create custom collections with emoji + name
- **Reading Challenges** — monthly (read 3/5/10) + theme (SNES Scholar, PS1 Pioneer etc) with progress bars
- **Because You Read** — client-side recommendations by platform/era
- In-reader comments — magazine-level and page-level with page number tagging
- 1–5 star ratings + text reviews saved to Firestore
- Internet Archive integration with search
- Bookmark magazines to Favorites

### 🎵 Albums
- **Featured Today hero cards** — random community + archive picks with SHUFFLE button
- **Now Playing bar** — persists while browsing with pulsing indicator
- **Era filter chips** — NES / SNES / PS1 / PS2 / N64 / GCN / GBA / NDS / PC
- Bigger cards (160dp) with play button overlay and gradient
- Community albums (7 curated) + Internet Archive search
- Album player with dark cinematic NOW PLAYING strip
- Bookmark albums to Favorites

### 📝 Articles
- **Featured article hero card** — highest view count article with cinematic overlay
- **Full article detail screen** — hero image, drop cap, highlighted snippet, full content
- **Category filter chips** — ALL / RETRO / GAMING / MUSIC / CULTURE / PIXEL ART
- Reading time estimate + view count on every card
- 🔥 TRENDING badge on articles with 50+ views
- QUICK PREVIEW inline expand + READ FULL → full screen
- Article editor with image upload, YouTube embed, preview mode
- Community + Internet Archive sections
- Bookmark articles to Favorites

### 📺 Streams
- **3 tabs:** LIVE / VIDEOS / COMMUNITY
- Twitch live streams (retro gaming category)
- YouTube retro gaming videos
- Community streamers with live detection
- OkHttp for all API calls on IO thread

### 🎮 Game Database
- **8 popular categories** loaded in parallel: Mario, Zelda, Sonic, Donkey Kong, Pokémon, Metroid, Castlevania, Street Fighter
- **3 tabs in detail:** INFO / TRAILER / SCREENSHOTS
- **YouTube trailer** — auto-searches `{game name} official trailer gameplay`
- Genre filter chips with keyword matching
- Color-coded IGDB rating (green/yellow/red) with score bar
- List view (cover + summary + rating) + Grid view (cover art + rating badge)
- Toggle list/grid with header icon button
- Loading progress bar while fetching categories

### 📅 Events
- Retro gaming anniversaries calendar (12 hardcoded: Mario, Zelda, Sonic, PS1 etc)
- Month grid calendar with event dots, day selection, prev/next navigation
- Community events from Firestore
- Add Event sheet — emoji picker, title, description, month/day
- Selected day + This Month + All Anniversaries sections

### 🛒 Marketplace
- Browse listings: FOR SALE / FOR TRADE / WANTED with color-coded filter pills
- Listing cards: image, type badge, title, platform, price, condition, seller avatar
- Listing detail: hero image, full details, seller card
- MESSAGE SELLER — opens RetroHub DM directly
- Tap seller → view their profile
- Add listing: image upload to Firebase Storage, platform chips, condition pills
- Only setupComplete users can post listings

### ⭐ Favorites & Bookmarks
- Bookmark magazines, albums, articles across all screens
- All saved to Firestore under user collection
- Favorites tab in Profile

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + StateFlow |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage (Blaze) |
| Game Data | IGDB API + Twitch OAuth |
| Streams | Twitch Helix API + YouTube Data API v3 |
| Content | Internet Archive API |
| Image Loading | Coil 2.6 |
| Video | AndroidYouTubePlayer + WebView |
| Networking | OkHttp 4.12 |
| Async | Kotlin Coroutines + StateFlow |

---

## 🔑 API Keys & Config

| Service | Where |
|---|---|
| Firebase | `app/google-services.json` |
| IGDB Client ID | `IGDBRepository.kt` |
| IGDB Secret | `IGDBRepository.kt` |
| Twitch (same app) | `StreamsViewModel.kt` |
| YouTube Data API v3 | `StreamsViewModel.kt` + `GameDatabaseScreen.kt` |

---

## 🗄️ Firestore Structure
