# RetroHub: Your Portal to Retro Gaming, Software & Soundtracks
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<!-- Optional: Add other badges like build status, version, etc. -->

## Overview
RetroHub is an Android application, built with Kotlin and Jetpack Compose, designed as a vibrant portal for enthusiasts of retro video games, software, hardware, and their iconic soundtracks. It aims to be a community-driven hub where users can discover, explore, and engage with content spanning classic eras to contemporary retro-inspired creations.

The app currently features a distinct retro-themed **Home Screen** that welcomes users, displays the **Latest News** fetched from an RSS feed, and provides navigation to core content sections: **Albums**, **Magazines**, and **Articles**. The project leverages modern Android development practices, emphasizing a clean, immersive retro aesthetic (vaporwave/synthwave influences) and a user-friendly interface. Future development will focus on populating these sections with rich content, implementing user accounts, and enabling community contributions.

## 🌟 Features

**Core User Interface & Navigation:**
- **🏠 Home Screen (`HomeScreen`):**
    *   A visually engaging entry point with a custom "HOME" title featuring prominent text shadows.
    *   A "WELCOME" section with a dynamic GIF (using Coil) and introductory text.
    *   **📰 Latest News Section (`NewsSection`):**
        *   Fetches and displays recent articles from an RSS feed (currently `retronews.com/feed/`).
        *   Presents news items in individual cards (`NewsItemCard`) showing title, summary, image (if available), source, and publication date.
        *   Handles loading states, error messages (with a retry option), and empty states.
        *   News cards are clickable, opening the article URL in a browser.
        *   Internally scrollable `LazyColumn` for displaying news items.
    *   Clearly defined navigation cards for "ALBUMS," "MAGAZINES," and "ARTICLES," each with unique gradient backgrounds, descriptions, and imagery.
    *   Interactive buttons on cards to navigate to respective sections (navigation logic is in place).
    *   A copyright footer with your name and a clickable link to your blog.
- **📚 Magazines Showcase (`MagazinesScreen` - In Progress):**
    *   Displays a "VIRTUAL MAGAZINES" title.
    *   Features scrollable virtual shelves populated with sample magazine covers.
    *   Magazine covers are clickable, preparing for navigation to a reader view.
- **📰 Articles & Reviews (Planned UI):**
    *   UI design planned for browsing articles related to retro video games, software, hardware, and industry history.
- **🎶 Albums & Soundtracks (Planned UI):**
    *   UI design planned for discovering and streaming soundtracks, viewing game/composer details, and album art.

**User & Community Features (Planned for Future Phases):**
- **👤 User Authentication**: Secure account creation (email/password) and login.
- **💾 Personalized Experience**: Saving favorites, tracking history, custom profiles.
- **✍️ Content Contribution**: Uploading magazines, albums, and submitting articles.
- **💬 Community Interaction**: Commenting, ratings, and forums.

**Technical & UI Highlights (Implemented & In Progress):**
- **🎨 Distinct Retro Theme**:
    *   Crafted vaporwave/synthwave aesthetic using custom fonts (`RetroFontFamily`).
    *   Specific color palettes (`RetroTextOffWhite`, `VaporwavePink`, `VaporwaveBlue`, `VaporwaveCyan`, `VaporwavePurple`, `SynthwaveOrange`).
    *   UI elements like text shadows (`TextStyle.shadow`), gradient backgrounds (`Brush.horizontalGradient`).
    *   Rounded corners, borders, and carefully chosen typography sizes and weights.
- **📱 Modern Android Stack**: Built with Kotlin and Jetpack Compose (Material 3).
- **📰 RSS Feed Integration**:
    *   `NewsViewModel` fetches and parses RSS feed data using `OkHttp` for network requests and `XmlPullParser` for XML parsing.
    *   `Jsoup` is used for HTML stripping and image extraction from feed content.
    *   Asynchronous data fetching with Kotlin Coroutines and `StateFlow`.
- **🖼️ Rich Media Presentation**:
    *   Dynamic GIF display on the Home screen using **Coil**.
    *   `AsyncImage` (from Coil) for loading news article images, magazine covers, etc.
- **Intuitive Navigation**:
    *   Implemented via a custom `RetroAppBar` with clickable text items that change color (`VaporwavePink`) upon selection.
    *   Navigation between `HomeScreen`, `MagazinesScreen`, `AlbumsScreen`, and `ArticlesScreen` is functional.

## 📸 Screenshots

*(Add your latest screenshots here! Ensure to include one of the News Section)*

| Home Screen (with News) | Magazine Shelf | Album Player (Planned) |
| :----------------------: | :--------------------: | :--------------------: |
| https://youtu.be/MX_IVb6w5dY |
| Article View (Planned) | User Profile (Planned) | App Bar Navigation |
| _[Image: ArticleDetailScreen (Mockup)]_ | _[Image: UserProfileScreen (Mockup)]_ | *[Image: RetroAppBar in action]* |


## 🛠️ Technologies & Concepts (Current & Planned)

- **Language**: Kotlin (100%)
- **Core Android & Jetpack Libraries**:
    *   **Jetpack Compose**: UI Toolkit (Material 3, Layouts, Foundation, Graphics, Animation).
        *   `LazyColumn` for efficient list display (e.g., News items).
        *   `AsyncImage` (from Coil) for image and GIF loading.
        *   `ClickableText` for interactive text elements (e.g., blog link).
        *   `Card` for structured content presentation (e.g., News items).
    *   **ViewModel (`AndroidViewModel`)**: For UI-related data lifecycle management (e.g., `NewsViewModel`).
    *   **Kotlin Coroutines & Flow (`StateFlow`)**: For asynchronous operations and reactive data streams.
    *   **`WebView` (via `AndroidView`)**: Planned for initial display of web-based magazine content.
    *   **ExoPlayer / Android Media3 (Planned)**: For robust audio streaming of game soundtracks.
    *   **Room Database (Planned)**: For local caching.
    *   **Retrofit / Ktor (Planned)**: For more complex backend API communication.
    *   **Jetpack Navigation Compose (Partially Implemented - Manual State Management Currently Used)**: Full adoption planned.
    *   **Hilt / Koin (Planned)**: For dependency injection.
- **Networking & Data Parsing**:
    *   **OkHttp**: For making HTTP requests (e.g., fetching RSS feed).
    *   **XmlPullParser**: For parsing XML data (RSS feed).
    *   **Jsoup**: For HTML parsing/stripping (from RSS descriptions).
- **Image Loading**:
    *   **Coil**: For efficient image loading and caching (covers, article images, album art, GIFs).
- **Backend Infrastructure (Conceptual - Requires Separate Development)**:
    *   RESTful API (Node.js/Express, Python/Django, Kotlin/Ktor, etc.).
    *   Database (PostgreSQL, MySQL, MongoDB).
    *   Cloud Storage (AWS S3, Firebase Storage, etc.).
- **UI/UX Principles**:
    *   Declarative UI paradigm with Jetpack Compose.
    *   State Hoisting and Unidirectional Data Flow.
    *   Material Design 3 guidelines adapted to the retro theme.
- **Data Handling**:
    *   Kotlin Data Classes (e.g., `NewsItem`).
    *   JSON (de)serialization (e.g., kotlinx.serialization - planned for future API interactions).

## 🚀 Getting Started

The project is under active development. Current functionality includes the Home screen with a welcome section and news feed, basic navigation, and the initial magazine shelf view.

### Prerequisites
- **Android Studio**: Latest stable version (e.g., Jellyfish, Iguana, or newer).
- **Android SDK**: API Level 24 (Nougat) or higher (adjust if your `minSdk` is different).
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Git**.
