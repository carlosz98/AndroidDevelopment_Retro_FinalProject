# RetroHub: Your Portal to Retro Gaming, Software & Soundtracks

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

RetroHub is an Android application built with Kotlin and Jetpack Compose, designed as a vibrant community portal for enthusiasts of retro video games, software, hardware, and their iconic soundtracks. It features a distinct vaporwave/synthwave aesthetic and serves as a hub where users can discover, explore, and engage with retro content — from classic gaming magazines and soundtracks to community-written articles and user profiles.

The app is powered by Firebase (Authentication, Firestore, Storage), the Internet Archive API, and IGDB for game data. Users can create profiles, follow each other, bookmark content, write articles, and explore a growing retro community.

---

## 🌟 Current Features

### 🎨 UI & Navigation
- Custom retro vaporwave/synthwave theme with `RetroFontFamily`, neon color palettes (`VaporwavePink`, `VaporwaveCyan`, `SynthwaveOrange`, etc.), text shadows, and gradient backgrounds
- Animated screen transitions (scale + fade) between all main sections
- Side drawer navigation with animated talking robot mascot that delivers contextual messages
- Screens: **Home**, **Discover**, **Magazines**, **Albums**, **Articles**, **Profile**

### 🏠 Home Screen
- Welcome section with retro navigation cards for Albums, Magazines, Articles and Profile
- Custom retro app bar with hamburger menu

### 👤 User Profiles & Auth
- Email/password and Google Sign-In via Firebase Auth
- Profile setup flow: choose username, top 6 games (via IGDB), top 3 soundtracks
- Full profile view with banner, avatar, bio, followers/following counts
- **Enhanced About section:**
  - Bio card
  - Location with 📍 icon
  - Clickable website link
  - "Member Since" date
  - Gaming platform bubbles (PlayStation, Xbox, Steam, Nintendo) with official SVG logos and platform colors
- Edit Profile supports all fields including platform usernames
- Followers / Following lists with search, follow/unfollow, and tap to view profile
- Recent Activity feed (bookmarks + articles written) stored in Firestore

### 🔍 Discover
- Real-time Firestore user search by username or handle
- User cards with avatar, bio preview, follow button, tap to open full profile
- Content search across magazines, albums, and articles
- Category hints shown before searching

### 📰 Magazines
- Virtual magazine shelf with scrollable covers
- Community + Internet Archive sections
- Bookmark magazines to Favorites

### 🎵 Albums
- Retro game soundtrack browser
- Community + Internet Archive sections  
- Bookmark albums to Favorites

### 📝 Articles
- Community articles + Internet Archive section
- Article editor with:
  - Title, snippet, full content with `**bold headings**` support
  - Header image (gallery upload to Firebase Storage OR paste image URL)
  - Optional YouTube video embed
  - Preview mode before publishing
- Floating **+** button for logged-in users
- Bookmark articles to Favorites

### 👥 Social & Follow System
- Follow / Unfollow users
- Follower and Following counts updated in real time via Firestore batch writes
- Find People overlay accessible from Follow/Following screen (PersonAdd icon)
- UserProfileViewScreen — read-only profile with games, soundtracks, and recent activity

### ⭐ Favorites & Bookmarks
- Bookmark magazines, albums, and articles
- All bookmarks saved to Firestore under user's collection
- Favorites tab in Profile screen
- Bookmarking automatically logs to Recent Activity

### 🔔 Recent Activity
- Logs bookmark events and article publications to Firestore
- Shown in Profile and on other users' profiles
- Time-ago formatting ("2 hours ago", "3 days ago", etc.)

### 🔧 Technical Highlights
- **Firebase**: Auth, Firestore, Firebase Storage (article images)
- **IGDB API**: Game search and cover art for profile setup
- **Internet Archive API**: Magazine and article content
- **Coil**: Image loading and GIF support
- **YouTube Player**: Embedded video in articles
- **OkHttp + Jsoup**: RSS/HTML parsing
- **Kotlin Coroutines + StateFlow**: Async data and reactive UI
- **Jetpack Compose Material 3**: Full declarative UI
- **AndroidViewModel**: Shared ViewModels across screens (Auth, Favorites, Activity, UserArticles)

---

## 🚧 Planned Features (Upcoming)

### 🔔 Enhanced Recent Activity
- "Just joined RetroHub!" event on account creation
- "Is now friends with @username" on follow
- "Wrote an article: [Title]" with inline clickable preview card

### 👤 Platform Usernames in Profile Setup
- Add PlayStation, Xbox, Steam, Nintendo username fields directly in the onboarding setup flow
- Same colorful platform bubbles shown immediately after setup

### 📖 In-App Magazine Reader
- Open magazines directly inside the app — no browser redirect
- Page-flip animation for an immersive reading experience (Kindle-style)

### 🎵 In-App Album Player
- Stream album audio directly within RetroHub
- Custom retro music player UI — no browser redirect

### 🏠 Home Screen Redesign
- Animated CRT scanline effect
- **"Now Trending"** carousel — top articles, albums, magazines
- **"Recently Added"** — latest community content with author avatars
- **"Featured Game"** — daily spotlight on a retro game with lore blurb
- **"Did You Know?"** — rotating retro gaming facts
- **"Retro of the Day"** — featured album, magazine or article
- **Retro Radio** — ambient chiptune/lofi music player embedded in home
- **Random Game Picker** — spin a wheel for a retro game suggestion
- Retro Quote of the Day

### 🔍 Discover — Recommendations & Trends
- Trending magazines, albums, articles, and users shown before searching
- Personalized recommendations based on user activity and bookmarks
- Each category in its own section

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + StateFlow |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Game Data | IGDB API |
| Content | Internet Archive API |
| Image Loading | Coil |
| Video | Android YouTube Player |
| Networking | OkHttp + Jsoup |
| Async | Kotlin Coroutines |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Latest stable version
- **Android SDK**: API Level 24 (Nougat) or higher
- **JDK**: Version 17 or higher
- **Git**
- **Firebase project** with Auth, Firestore, and Storage enabled
- **IGDB API credentials** (Client ID + Bearer token)

### Setup
1. Clone the repository
2. Open in Android Studio
3. Add your `google-services.json` to the `app/` directory
4. Add your IGDB credentials to `IGDBRepository.kt`
5. Build and run on an emulator or physical device

---

## 📸 Screenshots

*(Coming soon)*

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
