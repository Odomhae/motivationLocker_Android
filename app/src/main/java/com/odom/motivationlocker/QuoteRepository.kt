package com.odom.motivationlocker

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

object QuoteRepository {

    // language index -> assets/*.json 파일명 (MainActivity의 languageCategory 배열 순서와 일치: 0=영어, 1=한국어)
    private val LANGUAGE_FILES = mapOf(
        0 to "English.json",
        1 to "korean.json"
    )

    private val cache = mutableMapOf<Int, List<Quote>>()

    fun getRandomQuote(context: Context, language: Int): Quote {
        val quotes = cache.getOrPut(language) { loadQuotes(context, language) }
        return quotes[Random.nextInt(quotes.size)]
    }

    private fun loadQuotes(context: Context, language: Int): List<Quote> {
        val fileName = LANGUAGE_FILES[language] ?: LANGUAGE_FILES.getValue(0)
        val json = context.assets.open(fileName).reader().readText()
        val sayingArray = JSONArray(json)

        return (0 until sayingArray.length()).map { index ->
            val saying = sayingArray.getJSONObject(index)
            Quote(
                id = saying.getInt("id"),
                quote = saying.getString("quote"),
                writer = saying.getString("writer")
            )
        }
    }
}
