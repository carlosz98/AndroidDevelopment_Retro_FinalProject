package com.example.hubretro

import androidx.compose.animation.animateContentSize
// --- Animation Imports (existing) ---
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// --- End Animation Imports ---
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed // Ensure this is itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush // Added for gradient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.hubretro.ui.theme.* // Your theme imports, including articleGradientColorsList
// --- YouTube Player Imports ---
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

// Make sure these lines are REMOVED if they were previously added here:
// val ArticleCardLightGreyBackground = Color(0xFFEAEAEA)
// val ArticleCardDarkText = Color.Black.copy(alpha = 0.87f)
// val ArticleCardSecondaryText = Color.DarkGray.copy(alpha = 0.7f)
// val ArticleCardDividerColor = Color.Gray.copy(alpha = 0.4f)


// 1. Data Class (Should be Unchanged)
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val date: String? = null,
    val author: String? = null,
    val imageResId: Int? = null,
    val youtubeVideoId: String? = null
)

// 2. Sample Article Data (Unchanged - using your existing data)
val sampleArticles = listOf(
    ArticleItem(
        id = "1",
        title = "The Pixelated Pull: Why Retro Gaming is Booming Again",
        snippet = "Beyond nostalgia, discover the reasons for the resurgence of classic video games and their timeless appeal in a modern world.",
        fullContent = """
            The year 2024 isn't just about the next generation of hyper-realistic graphics; it's
            also witnessing an unprecedented boom in the popularity of retro gaming. From dusty attics to digital storefronts, classic titles from the 80s, 90s, and early 2000s are capturing the hearts of both seasoned gamers and a new generation of players. But what's fueling this pixelated renaissance?

            **More Than Just Memory Lane:**
            While nostalgia is undoubtedly a powerful catalyst, the retro revival runs deeper. For many who grew up with these games, it's a comforting return to simpler times, a way to reconnect with cherished childhood memories and the joy of discovering virtual worlds with friends. The distinct chiptune soundtracks, the challenging yet rewarding gameplay, and the iconic characters evoke a potent sense_of warmth and familiarity in an often overwhelming modern world.

            **The Allure of Simplicity and Challenge:**
            In an era of sprawling open worlds and complex game mechanics, retro games offer a refreshing directness. They often feature straightforward objectives, intuitive controls (mastered through practice!), and a level of challenge that demands skill and perseverance. This "easy to learn, hard to master" philosophy provides a unique satisfaction that can sometimes be lost in more contemporary titles. The triumph of finally beating that notoriously difficult boss or achieving
            a high score resonates deeply.

            **Accessibility and Community:**
            The rise of emulation, dedicated retro consoles (like the Analogue Pocket or Evercades), and online communities has made these classics more accessible than ever. Players can easily revisit old favorites or discover hidden gems they missed the first time around. Online forums, social media groups, and retro gaming conventions foster a vibrant community where enthusiasts share their passion, trade tips, and celebrate the rich history of gaming.

            **Timeless Design and Innovation:**
            Many retro games are lauded for their innovative game design and artistic vision, achieved despite significant hardware limitations. Developers of that era were masters of making the most out of minimal resources, leading to creative gameplay loops and iconic pixel art styles that remain influential today. This enduring quality is a testament to their craftsmanship.

            The retro gaming comeback isn't just a fleeting trend; it's a celebration of gaming's foundations, a testament to the timeless appeal of good design, and a powerful reminder that sometimes, looking back is the best way to move forward in our appreciation of interactive entertainment.
        """.trimIndent(),
        date = "Nov 15, 2023",
        author = "Don Carlos",
        imageResId = R.drawable.article1, // Ensure you have these drawables
        youtubeVideoId = "fuSRjyR_ZJU"
    ),
    ArticleItem(
        id = "2",
        title = "The Digital Ghosts: Exploring the World of Abandonware",
        snippet = "Unearthing lost classics and forgotten gems from the digital past. What happens when software is left behind?",
        fullContent = """
            In the fast-paced world of software development, titles that once graced magazine covers and topped sales charts can eventually fade into obscurity, no longer sold or supported by their original creators. This is the realm of **abandonware**: software, typically games, that is no longer commercially available and for which official support has ceased. But far from being digital graveyards, abandonware communities are vibrant hubs of preservation and nostalgia.

            **What Qualifies as Abandonware?**
            The definition can be murky, as copyright technically still applies to most of these works. Generally, software is considered abandonware if:
            - It's no longer sold through official channels.
            - The copyright holder no longer actively enforces their copyright or provides support.
            - It often requires emulation or community patches to run on modern hardware.
            It's a gray area legally, but one driven by a passion for preserving digital heritage.

            **Why the Enduring Appeal?**
            The fascination with abandonware stems from several factors:
            - **Nostalgia:** For many, it's a chance to revisit formative gaming experiences from their youth.
            - **Historical Significance:** These games are artifacts of computing history, showcasing early innovations in gameplay, graphics, and sound.
            - **Accessibility:** Abandonware sites often provide easy access to games that would otherwise be impossible to find or play.
            - **Unique Experiences:** Many older games offer gameplay mechanics and artistic styles not commonly found in modern titles.
            - **Community Effort:** The dedication of fans who archive these games, write patches, and provide support is a powerful draw.

            **The Preservationist's Dilemma:**
            While abandonware sites offer a lifeline to these digital ghosts, they operate in a complex legal and ethical space. The ideal scenario involves copyright holders officially releasing their older titles for free or into the public domain. However, until then, abandonware communities serve as unofficial digital archaeologists, ensuring that these important pieces of software history aren't lost to time or bit rot.

            Exploring abandonware is like stepping into a time capsule, offering a unique window into the evolution of interactive entertainment and the enduring power of digital experiences.
        """.trimIndent(),
        date = "July 29, 2025",
        author = "Topin99",
        imageResId = R.drawable.article2, // Ensure you have these drawables
        youtubeVideoId = "onP3tHaHmQs"
    ),
    ArticleItem(
        id = "3",
        title = "The Serene Symphony of Survival: Minecraft's Enduring Soundtrack",
        snippet = "Exploring the subtle genius of C418's compositions and how they define the Minecraft experience.",
        fullContent = """
            Beyond the blocky landscapes and endless creative possibilities, one of the most iconic and beloved aspects of Minecraft is its unique and evocative soundtrack, primarily composed by Daniel Rosenfeld, also known as C418. It's a score that doesn't just accompany gameplay; it defines it.

            **A World of Calm and Wonder:**
            Unlike the bombastic scores of many action-packed games, Minecraft's music is predominantly ambient, minimalist, and deeply atmospheric. Tracks like "Sweden," "Minecraft," and "Subwoofer Lullaby" are gentle piano melodies and soft synth pads that evoke feelings of peace, solitude, and wonder. They create a soundscape that is both calming and subtly melancholic, perfectly complementing the game's focus on exploration and building in vast, often empty, landscapes.

            **The Genius of Subtlety:**
            The power of C418's work lies in its subtlety. The music often fades in and out, never overstaying its welcome or becoming intrusive. It provides a sense of companionship during solo adventures and a tranquil backdrop for creative endeavors. The occasional shift to more unsettling or mysterious tracks, like those found in caves or the Nether, effectively heightens tension without resorting to cliché horror tropes.

            **More Than Just Background Noise:**
            For millions of players, the Minecraft soundtrack is inextricably linked to cherished memories: the quiet satisfaction of building a first home, the thrill of discovering a rare ore, or the peacefulness of watching a pixelated sunset. It has transcended the game itself, becoming a staple for studying, relaxing, or simply reminiscing.

            **An Enduring Legacy:**
            Even as Minecraft has evolved and new music has been added by other composers like Lena Raine and Kumi Tanioka, C418's original compositions remain the heart and soul of the game's auditory identity. They are a testament to the idea that sometimes, the most powerful art is that which whispers rather than shouts, creating an emotional resonance that lasts for years. The serene symphony of Minecraft continues to enchant players, old and new.
        """.trimIndent(),
        date = "July 28, 2025",
        author = "HomicidalYellio",
        imageResId = R.drawable.article3, // Ensure you have these drawables
        youtubeVideoId = "9EvH-2e5at4"
    ),
    ArticleItem(
        id = "4",
        title = "The Charm of Simplicity: Why Modern Games Embrace the Low-Polygon Aesthetic",
        snippet = "Exploring the resurgence of low-poly graphics, moving beyond nostalgia to become a deliberate and impactful art style.",
        fullContent = """
            In an era where photorealism often dominates the visual landscape of gaming, a distinct and compelling trend has emerged: the deliberate use of low-polygon aesthetics. Far from being a mere throwback or a limitation, modern low-poly design is a conscious artistic choice that offers unique advantages and resonates deeply with both developers and players.

            **What is Low-Poly?**
            The term "low-poly" refers to 3D models constructed with a relatively small number of polygons (the flat, basic shapes that form the surfaces of 3D objects). This results in a distinct, often stylized and somewhat abstract look, characterized by sharp edges, flat shading, and clearly defined geometric forms. Think of early 3D games like those on the original PlayStation or Nintendo 64, but refined with modern rendering techniques.

            **Beyond Nostalgia: A Deliberate Choice:**
            While nostalgia for early 3D gaming certainly plays a role in its appeal, the modern resurgence of low-poly is driven by more than just fond memories:
            - **Artistic Expression & Style:** Low-poly allows for highly stylized and evocative visuals. It can create dreamlike, minimalist, or even surreal atmospheres that might be harder to achieve with hyperrealism. The simplicity can be strikingly beautiful and allows artists to focus on color, form, and composition.
            - **Performance Benefits:** Fewer polygons mean less computational load. This makes low-poly an excellent choice for indie developers with limited resources, mobile games, or games aiming for high frame rates on a wide range of hardware.
            - **Clarity and Readability:** The clean, uncluttered look of low-poly graphics can enhance gameplay clarity. Important objects and characters often stand out more clearly against simplified backgrounds, making for a more readable and less distracting player experience.
            - **Faster Development Cycles:** Creating low-poly assets can be significantly faster than crafting highly detailed, realistic models. This allows smaller teams to produce more content or iterate more quickly on their designs.
            - **Timeless Appeal:** Unlike hyperrealism, which can quickly look dated as technology advances, stylized low-poly art has a more timeless quality. Games like "Journey," "Monument Valley," or "Totally Accurate Battle Simulator" showcase the enduring beauty and effectiveness of this approach.

            **The Future is Geometric:**
            The low-polygon aesthetic is not just a retro revival; it's a versatile and powerful tool in a game developer's arsenal. It proves that visual appeal isn't solely dependent on polygon count. By embracing simplicity, developers can create unique, memorable, and performant gaming experiences that captivate players with their distinct charm and artistic vision.
        """.trimIndent(),
        date = "Aug 1, 2025",
        author = "Carollerm",
        imageResId = R.drawable.article4, // Ensure you have these drawables
        youtubeVideoId = "9E0XPzB9wZU"
    ),
    ArticleItem(
        id = "5",
        title = "The Enduring Charm of Pixels: Why Pixel Art is Still Gorgeous",
        snippet = "Exploring the timeless appeal of pixel art, from its retro roots to its modern masterpieces, and why this art form continues to captivate.",
        fullContent = """
            In a world chasing photorealism, there's an undeniable and enduring charm to pixel art. What began as a necessity due to hardware limitations has evolved into a deliberate and beloved art style, capable of evoking nostalgia, conveying complex emotions, and showcasing incredible artistic skill. Pixel art isn't just retro; it's a timeless medium that continues to produce gorgeous and memorable visuals.

            **The Beauty in Limitation:**
            One of the core appeals of pixel art is the artistry born from constraint. Every pixel is placed with intention. Artists must make careful choices about color palettes, shading, and form to create recognizable and evocative imagery within a limited grid. This deliberate process often leads to:
            - **Clarity and Readability:** Well-crafted pixel art is incredibly clear. Characters and environments are distinct and instantly understandable.
            - **Unique Aesthetics:** From the chunky charm of 8-bit sprites to the detailed intricacy of modern high-bit pixel art, the style has a unique visual signature that can't be replicated by other means.
            - **Evocative Power:** Pixel art has a unique ability to tap into our imaginations, allowing us to fill in the details and connect with the artwork on a personal level. The simplicity can be surprisingly expressive.

            **Beyond Nostalgia: A Thriving Modern Art Form:**
            While many associate pixel art with classic arcade games and 80s/90s consoles, it's a vibrant and evolving art form today:
            - **Indie Game Darling:** Many independent game developers choose pixel art for its aesthetic appeal, its relative speed of asset creation for small teams, and its ability to create unique game worlds. Titles like "Stardew Valley," "Celeste," and "Hyper Light Drifter" are testaments to its modern power.
            - **Expressive Storytelling:** Pixel art can tell profound stories and create deeply atmospheric experiences. The style can range from cute and whimsical to dark and moody.
            - **Skill and Dedication:** Creating high-quality pixel art requires immense skill, patience, and an eye for detail. Modern pixel artists push the boundaries with stunning animations, intricate backgrounds, and expressive character designs.

            **Why We Still Love It:**
            Pixel art connects with us on multiple levels. It can be a warm hug of nostalgia for those who grew up with it, or a fresh and stylish aesthetic for new audiences. It proves that graphical fidelity isn't the only measure of beauty; the careful placement of each tiny square can create worlds as immersive and breathtaking as any high-polygon render. The pixel art vibe is strong, and its capacity for gorgeousness is limitless.
        """.trimIndent(),
        date = "July 12, 2025",
        author = "LadiesMan61",
        imageResId = R.drawable.article5, // Ensure you have these drawables
        youtubeVideoId = "lT9VVMF10Hk"
    ),
    ArticleItem(
        id = "6",
        title = "Checking In Again: The Enduring Nostalgia of Habbo Hotel",
        snippet = "Remember Wobes, Furni, and the iconic Pool? A look back at Habbo Hotel and why its pixelated world still holds a special place in our hearts.",
        fullContent = """
            For a certain generation that came of age in the early 2000s, the words "Bobba," "Furni," and "Pool's Closed" evoke an instant wave of nostalgia. Habbo Hotel, the pixelated social MMO, wasn't just a game; it was a vibrant online world, a digital hangout spot where millions forged friendships, traded virtual goods, and navigated the complex social dynamics of its iconic rooms.

            **A Pixelated Universe of Possibilities:**
            Launched in 2000 by Sulake, Habbo Hotel offered a unique proposition: a virtual hotel where users could create their own avatar, design and decorate guest rooms, play games, and simply chat with people from around the globe. The charm was in its simplicity and the freedom it offered. Whether you were meticulously arranging your virtual furniture to create the coolest room, trying to scam your way to a rare Dragon Lamp, or just chilling in the Welcome Lounge, Habbo was an experience.

            **The Social Fabric:**
            More than the games or the collectibles, Habbo's enduring legacy is its social aspect. It was a place where you could be anyone, try out different personas, and connect with peers outside your immediate physical environment. Friendships were formed, alliances were made in games like Wobble Squabble or BattleBall, and the hotel buzzed with the constant chatter of its inhabitants. Of course, it also had its share of digital drama, from room raids to the infamous scams, all part of the unique Habbo ecosystem.

            **Why We Still Remember It Fondly:**
            - **Sense of Community:** Despite its vastness, Habbo fostered a sense of belonging for many.
            - **Creative Expression:** Room design was a huge part of the appeal, allowing users to showcase their creativity (and wealth!).
            - **Early Online Identity:** For many, Habbo was one of their first forays into creating an online identity and interacting in a persistent virtual world.
            - **Simpler Times:** In today's complex digital landscape, the straightforward nature of Habbo's interactions and its charmingly dated pixel art feels refreshingly simple.

            While the official Habbo Hotel still exists, and various private servers (or "retros") attempt to recapture its golden age, the original experience remains a cherished memory. It was a unique cultural moment, a pixelated microcosm of teenage life, and a testament to the power of online communities, one "Bobba" at a time.
        """.trimIndent(),
        date = "August 05, 2024", // Example date
        author = "Fabriko98",
        imageResId = R.drawable.article6, // **NEW DRAWABLE NEEDED**
        youtubeVideoId = "RCATF_Y3VAE" // Example: Habbo Hotel - Official Trailer (Old one if possible)
    ),

    )

// --- YoutubePlayerCard --- (Unchanged from your original, assuming it works as intended)
@Composable
fun YoutubePlayerCard(
    youtubeVideoId: String?,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val context = LocalContext.current
    val youTubePlayerView = remember { YouTubePlayerView(context) }

    DisposableEffect(lifecycleOwner, youtubeVideoId, youTubePlayerView) {
        if (youtubeVideoId.isNullOrBlank()) {
            youTubePlayerView.release()
            return@DisposableEffect onDispose {}
        }

        val playerListener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(youtubeVideoId, 0f)
            }
        }

        youTubePlayerView.enableAutomaticInitialization = false
        val playerOptions = IFramePlayerOptions.Builder().build()
        youTubePlayerView.initialize(playerListener, playerOptions)
        lifecycleOwner.lifecycle.addObserver(youTubePlayerView)

        onDispose {
            youTubePlayerView.release()
            lifecycleOwner.lifecycle.removeObserver(youTubePlayerView)
        }
    }

    if (!youtubeVideoId.isNullOrBlank()) {
        AndroidView(
            factory = { youTubePlayerView },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Transparent)) // Ensure placeholder is transparent
    }
}


// --- StyledArticleContentWithLargeInitial --- (Unchanged, text colors are passed in)
@Composable
fun StyledArticleContentWithLargeInitial(
    text: String,
    defaultStyle: TextStyle,
    subheadingStyle: SpanStyle,
    largeInitialStyle: SpanStyle
) {
    if (text.isEmpty()) {
        Text("", style = defaultStyle)
        return
    }

    val annotatedString = buildAnnotatedString {
        withStyle(style = largeInitialStyle) {
            append(text.first())
        }
        val restOfText = text.substring(1)
        val regex = """\*\*(.*?)\*\*""".toRegex() // Regex for **bolded** subheadings
        var lastIndex = 0
        regex.findAll(restOfText).forEach { matchResult ->
            val subheadingText = matchResult.groups[1]?.value ?: ""
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            // Append text before the subheading
            if (startIndex > lastIndex) {
                withStyle(defaultStyle.toSpanStyle()) {
                    append(restOfText.substring(lastIndex, startIndex))
                }
            }
            // Append the subheading with its style
            withStyle(style = subheadingStyle) {
                append(subheadingText)
            }
            lastIndex = endIndex
        }
        // Append any remaining text after the last subheading
        if (lastIndex < restOfText.length) {
            withStyle(defaultStyle.toSpanStyle()) {
                append(restOfText.substring(lastIndex))
            }
        }
    }
    Text(
        text = annotatedString,
        style = defaultStyle // The overall style for the Text composable
    )
}


// 3. ArticleCard Composable (MODIFIED for gradient background and light text)
@Composable
fun ArticleCard(
    article: ArticleItem,
    gradientColors: List<Color>, // Added back
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val infiniteTransition = rememberInfiniteTransition(label = "card_shadow_wiggle_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "cardShadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "cardShadowOffsetY"
    )

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.linearGradient(gradientColors)) // MODIFIED: Use gradient
            .border(
                width = 1.dp,
                color = RetroBorderColor.copy(alpha = 0.5f), // Adjusted for gradients
                shape = cardShape
            )
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        // --- 1. Image Section (Top) ---
        article.imageResId?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Header image for ${article.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    .border(1.dp, VaporwavePink),
                contentScale = ContentScale.Crop
            )
            Divider(
                color = RetroTextOffWhite.copy(alpha = 0.3f), // MODIFIED: Light divider
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // --- 2. Text Content Section (Middle) ---
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (article.imageResId == null) 16.dp else 8.dp,
                bottom = 8.dp
            )
        ) {
            Text(
                text = article.title.uppercase(),
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite, // MODIFIED: Light text
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f), // Darker shadow for gradients
                        offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                        blurRadius = 1.5f
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                val defaultFullContentStyle = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.9f), // MODIFIED: Light text
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                StyledArticleContentWithLargeInitial(
                    text = article.fullContent,
                    defaultStyle = defaultFullContentStyle,
                    subheadingStyle = SpanStyle(
                        fontFamily = RetroFontFamily,
                        color = VaporwavePink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                    largeInitialStyle = defaultFullContentStyle.toSpanStyle().copy(
                        fontSize = defaultFullContentStyle.fontSize?.times(2.5) ?: 35.sp,
                        fontWeight = FontWeight.Bold,
                        color = RetroTextOffWhite, // MODIFIED: Light text
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 1f
                        )
                    )
                )
            } else {
                Text(
                    text = article.snippet,
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.85f), // MODIFIED: Light text
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) "READ LESS..." else "READ MORE...",
                fontFamily = RetroFontFamily,
                color = VaporwavePink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )

            article.date?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Published: $it",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.7f), // MODIFIED: Light text
                    fontSize = 12.sp
                )
            }

            article.author?.let { authorName ->
                Spacer(modifier = Modifier.height(if (article.date != null) 4.dp else 8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.7f), // MODIFIED: Light text
                            fontSize = 12.sp
                        )) {
                            append("Written by: ")
                        }
                        withStyle(style = SpanStyle(
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )) {
                            append(authorName)
                        }
                    }
                )
            }
        } // End of Text Content Section

        // --- 3. YouTube Video Section (Bottom) ---
        val hasVideo = !article.youtubeVideoId.isNullOrBlank()
        val showVideoPlayer = isExpanded && hasVideo

        if (showVideoPlayer) {
            Divider(
                color = RetroTextOffWhite.copy(alpha = 0.3f), // MODIFIED: Light divider
                thickness = 1.dp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            YoutubePlayerCard(
                youtubeVideoId = article.youtubeVideoId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .border(1.dp, VaporwavePink),
                lifecycleOwner = lifecycleOwner
            )
        } else if (!isExpanded && hasVideo && article.imageResId == null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)) {
                Divider(
                    color = RetroTextOffWhite.copy(alpha = 0.2f), // MODIFIED
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Video available when expanded",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f), // MODIFIED
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp)) // Consistent bottom padding
        }
    }
}



// 4. ArticlesScreen Composable (MODIFIED to pass gradients and use light title text)
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier,
    articles: List<ArticleItem> = sampleArticles
) {
    val infiniteTransition = rememberInfiniteTransition(label = "screen_title_shadow_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "screenTitleShadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "screenTitleShadowOffsetY"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            // .background(RetroDarkBlue) // Optional: Set screen background if not by theme
            .padding(16.dp)
    ) {
        Text(
            text = "LATEST ARTICLES",
            fontFamily = RetroFontFamily,
            color = RetroTextOffWhite, // MODIFIED: Use light text for title
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = VaporwavePink.copy(alpha = 0.7f),
                    offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                    blurRadius = 0.5f
                )
            ),
            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(
                items = articles,
                key = { _, article -> article.id }
            ) { index, article ->
                val currentGradientColors = articleGradientColorsList[index % articleGradientColorsList.size]
                ArticleCard(
                    article = article,
                    gradientColors = currentGradientColors, // MODIFIED: Pass the gradient
                    initiallyExpanded = false
                )
            }
        }
    }
}


// 5. Previews (MODIFIED - change background to dark for previews)

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D, name = "Expanded Card (Dark BG)")
@Composable
fun ArticleCardPreviewExpandedWithVideoDarkBg() {
    HubRetroTheme {
        Box(Modifier.padding(16.dp).background(RetroDarkBlue)) {
            val previewArticle = sampleArticles.firstOrNull { it.id == "1" } ?: sampleArticles.first()
            ArticleCard(
                article = previewArticle,
                gradientColors = articleGradientColorsList.firstOrNull() ?: listOf(VaporwavePink, VaporwaveBlue),
                initiallyExpanded = true
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D, name = "Collapsed Card (Dark BG)")
@Composable
fun ArticleCardPreviewCollapsedWithVideoDarkBg() {
    HubRetroTheme {
        Box(Modifier.padding(16.dp).background(RetroDarkBlue)) {
            val previewArticle = sampleArticles.firstOrNull { it.id == "1" } ?: sampleArticles.first()
            ArticleCard(
                article = previewArticle,
                gradientColors = articleGradientColorsList.getOrElse(1) {articleGradientColorsList.firstOrNull() ?: listOf(VaporwavePink, VaporwaveBlue)},
                initiallyExpanded = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D, name = "Card Image Only (Dark BG)")
@Composable
fun ArticleCardPreviewWithImageDarkBg() {
    HubRetroTheme {
        Box(Modifier.padding(16.dp).background(RetroDarkBlue)) {
            val previewArticle = sampleArticles.firstOrNull { it.id == "3" } ?: sampleArticles.first { it.imageResId != null }
            ArticleCard(
                article = previewArticle,
                gradientColors = articleGradientColorsList.getOrElse(2) {articleGradientColorsList.firstOrNull() ?: listOf(VaporwavePink, VaporwaveBlue)}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D, name = "Card No Media (Dark BG)")
@Composable
fun ArticleCardPreviewNoMediaDarkBg() {
    HubRetroTheme {
        Box(Modifier.padding(16.dp).background(RetroDarkBlue)) {
            val previewArticle = sampleArticles.firstOrNull { it.imageResId == null && it.youtubeVideoId == null }
                ?: ArticleItem(
                    id = "no-media-preview-dark",
                    title = "Text Only Article (Dark)",
                    snippet = "This is a short snippet for a text-only article on a dark gradient.",
                    fullContent = "This is the full content for the text-only article. It demonstrates how the card looks when expanded on a gradient background with light text. Subheadings like **This One Here** would be styled too.",
                    date="Jan 1, 2024",
                    author="Preview Author"
                )
            ArticleCard(
                article = previewArticle,
                gradientColors = articleGradientColorsList.getOrElse(3) {articleGradientColorsList.firstOrNull() ?: listOf(VaporwavePink, VaporwaveBlue)}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D, name = "Articles Screen (Dark BG)")
@Composable
fun ArticlesScreenPreviewDarkContext() {
    HubRetroTheme {
        ArticlesScreen(articles = sampleArticles)
    }
}


