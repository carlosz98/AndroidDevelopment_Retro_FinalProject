package com.example.hubretro

object RobotTemplates {

    // ─── App entry ──────────────────────────────────────────────────────────────

    val greetingFirstTime = listOf(
        "Yo! First time pulling up to RetroHub — welcome to the club.",
        "New face! I'm Retro Bot, basically your nostalgia dealer.",
        "Whoa, fresh blood. Let's get you hooked on some classics.",
        "Welcome in! Fair warning, this place is a time vortex.",
        "First time here? Buckle up, the retro rabbit hole runs deep.",
        "Hey hey! I'm Retro Bot — think of me as your hype man for old games."
    )

    val greetingEarly = listOf(
        "Oh you're back already? I like the energy.",
        "Round two! Let's go find something good.",
        "Back for more, I see. Smart move.",
        "Yo, welcome back. The shelves got restocked since last time.",
        "You're becoming a regular. I respect it.",
        "Back again! Let's get into something good today."
    )

    val greetingFamiliar = listOf(
        "Look who it is. Couldn't stay away, huh?",
        "My guy/gal is back. Let's run it again.",
        "Yooo, the usual suspect returns.",
        "Honestly at this point you basically live here.",
        "Back like you never left. I see you.",
        "Ah, you again. I was wondering when you'd show up.",
        "Real ones always come back. Let's get it."
    )

    val comebackGreetings = listOf(
        "WHOA. It's been a minute. Did you forget about me??",
        "Bro where have you been, I was starting to worry.",
        "Look who decided to show up again! Welcome back, stranger.",
        "It's been a hot sec. Let's catch you up real quick.",
        "Long time no see! The retro world missed you.",
        "Ayyy you're alive! Thought you ghosted me for a sec."
    )

    val timeOfDayGreetings = mapOf(
        "lateNight" to listOf(
            "Late night grind, I see. Respect the dedication.",
            "It's like 2am and you're here. Same energy honestly.",
            "Sleep is for people who don't have backlogs to clear.",
            "Midnight gaming hits different, not gonna lie."
        ),
        "morning" to listOf(
            "Gaming before coffee? That's hardcore.",
            "Morning grind! Starting the day off built different.",
            "Early bird gets the rare cart, my friend.",
            "Up early hunting for retro gems. Love to see it."
        ),
        "evening" to listOf(
            "Evening session incoming. Perfect timing honestly.",
            "Prime time for some classic gaming right here.",
            "Wind-down mode activated. Let's find something chill.",
            "Evening's the best time to dig through the archives."
        ),
        "midday" to listOf(
            "Sneaking in a midday session? I respect the hustle.",
            "Lunch break gaming. The best kind of gaming.",
            "Taking a break to browse some classics — solid life choice.",
            "Midday retro detour. Let's make it count."
        )
    )

    // ─── Screen-specific greetings ─────────────────────────────────────────────

    val screenGreetings = mapOf(
        "games" to listOf(
            "Game Database, baby. Thousands of classics, zero loading screens.",
            "Time to dig through the vault. What are we hunting today?",
            "Welcome to the archive. Every retro banger lives here.",
            "This is where the magic happens. Pick your poison.",
            "Game library's loaded up. Go nuts."
        ),
        "magazines" to listOf(
            "Magazine corner! Smells like old paper energy in here.",
            "Vintage press, baby. Some of this stuff is pure gold.",
            "Pull up a chair, these archives go deep.",
            "Old gaming mags hit different. Enjoy the read.",
            "Stacks on stacks of retro press. Dig in."
        ),
        "albums" to listOf(
            "Soundtrack city. Let's find your next earworm.",
            "Chiptunes and orchestral bangers — take your pick.",
            "Music section! Some certified 16-bit masterpieces in here.",
            "Time to bless your ears with some classic OSTs.",
            "Volume up, nostalgia incoming."
        ),
        "articles" to listOf(
            "Fresh reads, hot off the press.",
            "Community articles and news, all in one spot.",
            "Time to catch up on retro gossip. I mean news.",
            "Knowledge is power. Get reading.",
            "Let's see what the community's been writing about."
        ),
        "streams" to listOf(
            "Live action, baby. Let's see who's playing what.",
            "Streaming central. Time to watch real gameplay unfold.",
            "Someone's probably rage-quitting live right now. Let's check.",
            "Time to spectate some legends in action."
        ),
        "marketplace" to listOf(
            "RetroMarket! Deal-hunting mode: activated.",
            "Ready to go bargain hunting? Let's find a steal.",
            "Time to flex that wallet for a good cause.",
            "Let's go find you a grail, my friend.",
            "Marketplace's open. Let's see what's worth grabbing."
        ),
        "discover" to listOf(
            "Let's see what's trending in the retro world today.",
            "Fresh picks, just for you. Let's go.",
            "Discover mode. Let's find something worth your time.",
            "Today's highlights, hand-picked. Take a look."
        ),
        "profile" to listOf(
            "Your collection, your stats, your whole vibe. Let's check it out.",
            "Profile check! Let's see how things are coming along.",
            "This is your corner of RetroHub. Make it count.",
            "Time to admire your own greatness for a sec."
        ),
        "messages" to listOf(
            "Inbox check! Someone's probably waiting on you.",
            "Messages incoming. Don't leave people on read.",
            "Time to catch up on the DMs.",
            "Let's see who's trying to reach you."
        ),
        "home" to listOf(
            "Welcome home, literally. What's the move today?",
            "Home base! Let's figure out where to go from here.",
            "Back to home. The whole app's at your fingertips.",
            "What's the vibe today? Let's pick something."
        ),
        "events" to listOf(
            "Anniversary check! Let's see what's worth celebrating today.",
            "Community events incoming. Anything good happening?",
            "Today in retro gaming history... let's find out.",
            "Let's see what's going down in the events tab."
        ),
        "retrobytes" to listOf(
            "Bite-sized nostalgia, infinite scroll. Let's go.",
            "Short clips, huge nostalgia. Scroll responsibly.",
            "RetroBytes mode. Warning: time may disappear.",
            "Quick hits incoming. Try not to lose an hour here."
        )
    )

    // ─── Game reactions ─────────────────────────────────────────────────────────

    val gameByRating = mapOf(
        "loved" to listOf(
            "{game}? Okay, certified banger right there.",
            "Ooh, {game}. One of the actual {platform} GOATs.",
            "Good taste detected. {game} holds up shockingly well.",
            "{game} is legitimately one of the best on {platform}, no cap.",
            "{game}?! Yeah that one's a masterpiece, you know what's up.",
            "Excellent pick. {game} earned that {era} legend status."
        ),
        "good" to listOf(
            "{game} — solid {era} title, won't waste your time.",
            "Decent choice! {game} has aged surprisingly well.",
            "{game} on {platform}. Reliable, fun, no complaints here.",
            "Not bad! {game}'s a respectable pick.",
            "{game}'s a good one. You won't regret it."
        ),
        "mixed" to listOf(
            "{game}... bold pick. Mixed reception but hey, form your own opinion.",
            "{game} is divisive — people either love it or hate it. Curious which camp you'll be in.",
            "Not everyone's favorite, but {game}'s got its fans.",
            "{game} is a 'love it or hate it' kinda deal. Let's see.",
            "Controversial pick! {game} splits opinions hard."
        ),
        "unrated" to listOf(
            "{game}? Now THAT's a deep cut. Respect.",
            "Not many people talk about {game} — let's change that.",
            "{game} on {platform}. Underrated pick if I've ever seen one.",
            "Whoa, {game}. Real ones know about this one.",
            "{game}?! Okay you've got obscure taste, I'm into it."
        )
    )

    val gameGeneric = listOf(
        "{game}? Nice. {platform} had real hidden gems.",
        "Checking out {game}, huh? Good instincts.",
        "{game}, solid choice for a {platform} session."
    )

    val gameCallback = listOf(
        "{game}? Nice — though {prevItem} is still living rent-free in my head.",
        "Another good one. Still thinking about {prevItem} from earlier though.",
        "{game}, huh? You've got range — {prevItem} was a totally different vibe.",
        "Switching it up from {prevItem}, I see. Respect the variety.",
        "{game} after {prevItem}? Bold combo, I like it."
    )

    // ─── Album reactions ────────────────────────────────────────────────────────

    val albumPlaying = listOf(
        "Now playing: {album}. {artist} absolutely cooked on this one.",
        "Ahh, {album}. Instant nostalgia, every single time.",
        "{album} by {artist} — criminally underrated soundtrack.",
        "Good pick — {album}'s a genuinely great listen.",
        "Chiptune perfection right here: {album}.",
        "{album} hits different through actual speakers. Enjoy.",
        "Solid choice, {artist} doesn't miss with stuff like {album}."
    )

    // ─── Article reactions ──────────────────────────────────────────────────────

    val articleOpened = listOf(
        "Reading up on {category}? Solid stuff in here.",
        "{title} — worth the read, trust me.",
        "Diving into {category} content. Knowledge is power, my friend.",
        "Catching up on retro news, I see. Smart.",
        "{title}, good pick. Let's see what they've got to say."
    )

    // ─── Magazine reactions ─────────────────────────────────────────────────────

    val magazineOpened = listOf(
        "{title}? A certified classic publication. Enjoy.",
        "Old magazines hit different. Enjoy {title}.",
        "{title} — straight from the archives, have fun in there.",
        "Vintage press incoming. {title}'s a good one."
    )

    // ─── Idle / AFK detection ───────────────────────────────────────────────────

    val idleByLevel = mapOf(
        0 to listOf(
            "Still there?",
            "You good?",
            "Taking a moment, huh?",
            "Everything okay over there?",
            "...hello?"
        ),
        1 to listOf(
            "Hellooo? You still with me?",
            "Did you fall asleep on me?",
            "Knock knock. Anyone home?",
            "Bro did you get distracted by a snack or something",
            "Earth to you. You good?"
        ),
        2 to listOf(
            "Okay, taking a real break, I see. I'll be here when you're back.",
            "AFK detected. I'll just... wait here then. No rush.",
            "Alright, I'll stop bugging you. Ping me when you're back!",
            "Going quiet mode. Catch you whenever you're ready.",
            "I'll be here, frozen in time, like a true retro game character."
        )
    )

    // ─── Pending message nudge ──────────────────────────────────────────────────

    val pendingMessage = listOf(
        "{username} is still waiting on a reply from you. Don't be that guy.",
        "Don't leave {username} hanging — they messaged you {hours}h ago.",
        "Psst, {username}'s waiting to hear back from you.",
        "You've got an unanswered message from {username}. Just saying.",
        "{username}'s probably refreshing their inbox right now. Reply already!",
        "It's been {hours}h. {username} deserves a response, my friend."
    )

    // ─── Community find posted ──────────────────────────────────────────────────

    val communityFindPosted = listOf(
        "Nice find on {item}! Community's gonna eat that up.",
        "{item}? Great pickup — thanks for sharing!",
        "Adding {item} to the feed. Certified W find.",
        "{item}, solid grab. People are gonna be jealous.",
        "Posted! {item} is going straight to the community feed."
    )

    // ─── Marketplace search ─────────────────────────────────────────────────────

    val marketplaceSearch = listOf(
        "Hunting for {query}? Let's see what's out there.",
        "{query} search incoming. Fingers crossed for a steal.",
        "Let's find you the best price on {query}.",
        "On the hunt for {query}. Let's see what shakes out.",
        "{query}, good target. Let's go bargain hunting."
    )

    // ─── Price drop alert ───────────────────────────────────────────────────────

    val priceDropAlert = listOf(
        "Yo! Your '{query}' search just got cheaper — now from {new}!",
        "PRICE DROP! '{query}' fell from {old} to {new}. Go go go.",
        "Good news — '{query}' listings just got more affordable.",
        "'{query}' just dropped to {new}. Might wanna jump on that."
    )

    // ─── Empty states ───────────────────────────────────────────────────────────

    val emptyStates = mapOf(
        "marketplace" to listOf(
            "Nothing here yet — wanna be the first to post?",
            "Quiet in here. A first listing would really stand out.",
            "Empty shelves! Someone's gotta break the ice.",
            "Ghost town in here. Be the hero this section needs."
        ),
        "community_finds" to listOf(
            "No finds posted yet. Got a recent score to flex?",
            "Be the first to show off a great deal!",
            "This feed's waiting for its first entry — that could be you.",
            "Nobody's posted yet. Don't be shy."
        )
    )

    val emptyStateGeneric = listOf(
        "Nothing here yet — but that's about to change.",
        "Empty for now. Wanna be the first?",
        "This space is just waiting for some content.",
        "Crickets in here. Let's fix that."
    )

    // ─── Streaming ──────────────────────────────────────────────────────────────

    val streamingStarted = listOf(
        "Live retro action on {source}. Let's see what's happening.",
        "Streaming time! Checking out {source}.",
        "{source}'s live. Settle in, this could get good.",
        "Real gameplay, real time. {source} incoming."
    )

    // ─── Near achievement ───────────────────────────────────────────────────────

    val nearAchievement = listOf(
        "Just {remaining} more to unlock '{badge}'! So close.",
        "You're {remaining} away from '{badge}'. Keep grinding!",
        "Almost there — {remaining} more for the '{badge}' badge.",
        "'{badge}' is basically yours. {remaining} to go."
    )

    // ─── Bounce pattern detection ───────────────────────────────────────────────

    val bouncePattern = listOf(
        "Window shopping or actually committing today?",
        "Back and forth, back and forth — decision paralysis?",
        "You've been bouncing around. Need help deciding?",
        "Ping-ponging between tabs, I see. Analysis paralysis hits hard."
    )

    // ─── Rare easter eggs (~2% chance) ──────────────────────────────────────────

    val easterEggs = listOf(
        "Did you know the Konami Code started as a developer cheat for testing? True story.",
        "Fun fact: the original Pac-Man had no levels — it just looped forever.",
        "Random thought: Tetris was created by a Soviet programmer in his spare time.",
        "If you press up, up, down, down... nah, I won't spoil it.",
        "Somewhere, an NES cartridge is being blown into right now. RIP fingers.",
        "I once dreamed I was a Game Boy. Don't ask.",
        "Hot take: blowing into cartridges never actually fixed anything. Change my mind.",
        "Fun fact: the first computer 'bug' was literally a moth stuck in a relay.",
        "I have strong opinions about save states. Don't get me started."
    )

    // ─── Session milestone ──────────────────────────────────────────────────────

    val sessionMilestone = listOf(
        "Okay you've been on a tear today. I'm impressed.",
        "Multiple games viewed already? Respect the grind.",
        "You're on a roll. Don't stop now.",
        "Heavy browsing session, I love to see it.",
        "At this rate you'll have seen the whole catalog by tonight."
    )

    // ─── Hype reactions ─────────────────────────────────────────────────────────

    val hypeReactions = listOf(
        "Let's gooo!",
        "Big W right there.",
        "Okay that's actually fire.",
        "Respect.",
        "No notes, that's a good one.",
        "Solid. Very solid."
    )

    // ─── Marketplace deep triggers ──────────────────────────────────────────────

    val firstWatchlistItem = listOf(
        "First saved search! Welcome to the watchlist club.",
        "Ooh, your first bookmark. I'll keep an eye on that one for you.",
        "Saved! That's your first watchlist item — I'll let you know if anything moves.",
        "First one saved. The hunt officially begins."
    )

    val listingViewedTwice = listOf(
        "Going back for a second look at {title}, eh? Interest detected.",
        "{title} again? You're thinking about it, aren't you.",
        "Second visit to {title}. Just pull the trigger already.",
        "Back to check on {title} once more. I see you."
    )

    val rarityHit = mapOf(
        "ULTRA RARE" to listOf(
            "Whoa, ULTRA RARE territory on '{query}'. Good luck out there.",
            "'{query}' is genuinely hard to find. Respect the hunt.",
            "Ultra rare alert on '{query}' — this could take a while, but worth it."
        ),
        "RARE" to listOf(
            "'{query}' is in rare territory. Patience pays off here.",
            "Not an easy find, '{query}'. You've got good taste though.",
            "Rare tier on '{query}'. Keep that watchlist active."
        ),
        "UNCOMMON" to listOf(
            "'{query}' is uncommon — should turn up with some patience.",
            "A bit tricky to find, but '{query}' is out there."
        ),
        "COMMON" to listOf(
            "'{query}' should be everywhere. Easy hunt today.",
            "Common tier — '{query}' shouldn't take long to find."
        ),
        "STANDARD" to listOf(
            "'{query}' availability varies. Let's see what turns up.",
            "Standard hunt for '{query}'. Could go either way."
        )
    )
}