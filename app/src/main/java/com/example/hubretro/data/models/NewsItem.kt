package com.example.hubretro.data.models // Make sure this matches YOUR project's base package + .data.models

/**
 * Represents a single news article item.
 *
 * This data class holds all the relevant information parsed from the news feed
 * for one news entry.
 *
 * @property id A unique identifier for the news item. This could be from the feed's <id> tag,
 *              the article URL, or a generated ID if others are unavailable.
 * @property title The main headline or title of the news article.
 * @property summary A brief summary or the initial content of the article.
 *                   This might be plain text or HTML that needs further processing for display.
 * @property sourceName The name of the news source (e.g., "Time Extension").
 * @property sourceUrl The direct URL to the full news article on the source's website.
 * @property imageUrl An optional URL pointing to an image associated with the news article.
 *                    This can be null if no image is found or provided.
 * @property publishedDate The publication date of the article, represented as a Long (timestamp
 *                         in milliseconds since the Unix epoch). This allows for easy sorting
 *                         and formatting.
 * @property category An optional category or tag associated with the news article.
 *                    This can be null if the feed doesn't provide categorization.
 */
data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val sourceName: String,
    val sourceUrl: String,
    val imageUrl: String?, // Nullable: An image might not always be present
    val publishedDate: Long, // Using Long for timestamp (milliseconds since epoch) for easier sorting/formatting
    val category: String?    // Nullable: Category might not always be available
)
