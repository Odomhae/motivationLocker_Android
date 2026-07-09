package com.odom.motivationlocker

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import kotlin.random.Random

object QuoteRepository {

    // 언어 추가 체크리스트 (TODO.md #6 참고):
    // 1) app/src/main/assets/<파일명>.json 추가 (형식은 parseQuotes 참고)
    // 2) 아래 LANGUAGE_FILES에 새 인덱스로 항목 추가 — 기존 인덱스는 재사용/재배치 금지.
    //    "language" 값은 SharedPreferences에 정수로 영속되므로 순서가 바뀌면 기존 사용자 설정이 깨진다.
    // 3) res/values/strings.xml(및 values-ko-rKR 등)의 languageCategory 배열 끝에 항목 추가
    private val LANGUAGE_FILES = mapOf(
        0 to "English.json",
        1 to "korean.json"
    )

    private const val DEFAULT_LANGUAGE = 0

    // 모든 로딩이 실패했을 때의 최후 안전망. English.json의 id=1 항목과 동일한 실제 명언이다.
    private val FALLBACK_QUOTE = Quote(
        id = 1,
        quote = "Change your thoughts\nand\nyou change your world.",
        writer = "- Norman Vincent Peale"
    )

    private val cache = mutableMapOf<Int, List<Quote>>()

    fun getRandomQuote(context: Context, language: Int): Quote {
        val quotes = cache.getOrPut(language) { loadQuotes(context, language) }
        if (quotes.isEmpty()) return FALLBACK_QUOTE
        return quotes[Random.nextInt(quotes.size)]
    }

    private fun loadQuotes(context: Context, language: Int): List<Quote> {
        val fileName = LANGUAGE_FILES[language] ?: LANGUAGE_FILES.getValue(DEFAULT_LANGUAGE)
        return try {
            val json = context.assets.open(fileName).reader().readText()
            parseQuotes(json)
        } catch (e: IOException) {
            Log.w("QuoteRepository", "명언 asset을 열지 못함: $fileName", e)
            // 기본 언어가 아니었다면 기본 언어로 한 번 더 시도, 그마저 실패하면 빈 리스트(getRandomQuote가 FALLBACK_QUOTE 처리)
            if (language != DEFAULT_LANGUAGE) loadQuotes(context, DEFAULT_LANGUAGE) else emptyList()
        }
    }

    // asset 읽기와 분리해 순수 함수로 테스트 가능하게 함(QuoteRepositoryTest 참고).
    // 항목 하나가 깨져 있어도(필드 누락 등) 전체 배열을 실패시키지 않고 해당 항목만 건너뛴다 —
    // 향후 대량의 명언 데이터를 추가할 때 데이터 하나의 오류로 전체 언어가 죽는 것을 방지한다.
    internal fun parseQuotes(json: String): List<Quote> {
        val sayingArray = try {
            JSONArray(json)
        } catch (e: JSONException) {
            Log.w("QuoteRepository", "명언 JSON 파싱 실패", e)
            return emptyList()
        }

        return (0 until sayingArray.length()).mapNotNull { index ->
            try {
                val saying = sayingArray.getJSONObject(index)
                Quote(
                    id = saying.getInt("id"),
                    quote = saying.getString("quote"),
                    writer = saying.getString("writer")
                )
            } catch (e: JSONException) {
                Log.w("QuoteRepository", "명언 항목 파싱 실패(index=$index), 건너뜀", e)
                null
            }
        }
    }
}
