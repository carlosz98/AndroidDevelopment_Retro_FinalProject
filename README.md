feat: major app overhaul — sessions 1-6 complete

UI & Design
- Migrated entire design system from vaporwave to Scrapbook/Comic aesthetic
- New color palette: ScrapbookCream, ScrapbookYellow, ScrapbookDark, ScrapbookBorder
- ScrapbookCard() component with offset shadow used throughout all screens
- BangersFontFamily for headers, NunitoFontFamily for body text

Home Screen
- Hero section with full-width image, gradient overlay, EXPLORE NOW CTA
- Today in Retro Gaming section with rotating historical facts
- Community activity strip with live user avatars from Firestore
- Nav grid cards upgraded to 160dp with play button overlays
- Featured Albums + Magazines carousels with SEE ALL navigation
- Random Game Picker with animated spin, 30+ classic games
- Streams section restyled to scrapbook white card
- Retro Quote of the Moment, Did You Know? with refresh

Profile Screen
- Gaming personality radar chart (RPG/Action/Platformer/Shooter/Adventure/Arcade)
- Activity heatmap — 7x7 grid, 4 intensity levels, streak stats
- Pinned article — pin one of your own articles to your profile
- Stream bubbles — Twitch (purple) + YouTube (red) clickable channel buttons
- XP system with level badges and progress bar
- 5-step profile setup with banner/photo upload fix (putBytes instead of putFile)

Discover Screen
- Filter strip: ALL/PEOPLE/ARTICLES/MAGAZINES/ALBUMS/LIVE
- Featured article hero card (highest viewCount from Firestore)
- Trending Players stacked card with gold/silver/bronze rank badges
- Live Now section pulling from StreamsViewModel Twitch data
- All sections in horizontal LazyRow carousels with SEE ALL buttons
- DiscoverSectionRow replaces ScrapbookDiscoverHeader

Chat System (new)
- Real-time DMs + group chats via Firestore
- Emoji reactions, image sharing with blur/reveal safety
- Unread badge on Messages bottom nav tab
- ChatListScreen, ChatScreen, NewChatScreen

Streams
- Twitch live streams + YouTube videos via OkHttp on IO thread
- Community streamers tab with live detection
- Fixed 403 YouTube error (removed Android app restriction from API key)
- Fixed Twitch token failure (switched to OkHttp FormBody)

Magazines (6 new features)
- Continue Reading strip with Firestore progress sync
- My Shelves — create custom emoji collections
- Reading Challenges — monthly + theme with progress bars
- In-reader comments — magazine-level and page-level
- 1-5 star ratings + text reviews
- Because You Read — client-side era/platform recommendations

Albums
- Featured Today hero cards — random community + archive picks + SHUFFLE
- Now Playing bar — persists while browsing with pulsing dot
- Era filter chips: NES/SNES/PS1/PS2/N64/GCN/GBA/NDS/PC
- Bigger cards (160dp) with centered play button + gradient overlay
- Dark cinematic player screen with blurred cover art background

Articles
- Featured article hero card at top (highest viewCount)
- Full article detail screen with hero image, drop cap, snippet highlight
- Category filter chips: ALL/RETRO/GAMING/MUSIC/CULTURE/PIXEL ART
- Reading time estimate + view count + TRENDING badge (50+ views)
- QUICK PREVIEW + READ FULL dual action buttons

Game Database (new drawer screen)
- 8 popular categories loaded in parallel with progress bar
- INFO/TRAILER/SCREENSHOTS tabs in game detail
- YouTube trailer auto-search via YouTube Data API
- Color-coded IGDB rating with score bar and label
- Genre keyword filter, list/grid toggle

Events (new drawer screen)
- Month grid calendar with event dots and day selection
- 12 hardcoded retro gaming anniversaries
- Community events from Firestore with Add Event sheet
- Emoji picker, title, description, month/day fields

Marketplace (new drawer screen)
- FOR SALE / FOR TRADE / WANTED color-coded filter pills
- Image upload to Firebase Storage
- Listing detail with seller card, MESSAGE SELLER opens DM
- Only setupComplete users can post

Bug Fixes
- Fixed Discover screen missing from when block in MainActivity
- Fixed banner/profile picture upload (StorageException 403) — was Firestore rules in Storage tab
- Fixed profile picture mirroring into banner preview in ProfileSetupScreen
- Fixed IGDB secret key mismatch between IGDBRepository and StreamsViewModel
- Fixed top bar hiding on all Messages screens instead of only active chat
- Fixed drawer overflow with 7 items — switched to scrollable Column
- Added IGDB token auto-refresh with 401 retry
- Fixed YouTube 403 — removed Android app restriction from Google Cloud Console

Firebase
- Added Firestore rules for all collections
- Added correct Storage rules (service firebase.storage)
- New collections: magazine_ratings, magazine_comments, events, marketplace
- New subcollections: users/{uid}/reading_progress, users/{uid}/magazine_shelves
