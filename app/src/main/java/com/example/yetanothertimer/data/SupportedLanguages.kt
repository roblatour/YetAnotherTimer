package com.example.yetanothertimer.data

import java.util.Locale

data class Language(val tag: String, val autonym: String)

object SupportedLanguages {
    val all: List<Language> = listOf(
        Language("ar", "العربية"),
        Language("bn", "বাংলা"),
        Language("zh-TW", "繁體中文"), // Traditional Chinese
        Language("zh-CN", "简体中文"), // Simplified Chinese
        Language("cs", "Čeština"),
        Language("de", "Deutsch"),
        Language("el", "Ελληνικά"),
        Language("en", "English"),
        Language("es", "Español"),
        Language("fa", "فارسی"),
        Language("fil", "Filipino"),
        Language("fr", "Français"),
        Language("gu", "ગુજરાતી"),
        Language("he", "עברית"),
        Language("hi", "हिन्दी"),
        Language("hu", "Magyar"),
        Language("id", "Bahasa Indonesia"),
        Language("it", "Italiano"),
        Language("ja", "日本語"),
        Language("kn", "ಕನ್ನಡ"),
        Language("ko", "한국어"),
        Language("mr", "मराठी"),
        Language("ms", "Bahasa Melayu"),
        Language("ms-Arab", "بهاس ملايو"), // Malay (Jawi)
        Language("my", "မြန်မာ"),
        Language("nl", "Nederlands"),
        Language("pa", "ਪੰਜਾਬੀ"),
        Language("pl", "Polski"),
        Language("pt-PT", "Português (Portugal)"),
        Language("pt-BR", "Português (Brasil)"),
        Language("ro", "Română"),
        Language("ru", "Русский"),
        Language("sk", "Slovenčina"),
        Language("sw", "Kiswahili"),
        Language("ta", "தமிழ்"),
        Language("te", "తెలుగు"),
        Language("th", "ไทย"),
        Language("tr", "Türkçe"),
        Language("uk", "Українська"),
        Language("ur", "اردو"),
        Language("vi", "Tiếng Việt")
    ).distinctBy { it.tag }.sortedBy { it.autonym.lowercase(Locale.ROOT) }

    // Returns true if the BCP-47 language tag is a right-to-left script language
    // Heuristics: language codes commonly RTL OR explicit script subtag "Arab"
    fun isRtl(tag: String): Boolean {
        val t = tag.lowercase(Locale.ROOT)
        // Common RTL language codes
        val rtlLangs = setOf("ar", "he", "fa", "ur", "ps", "sd")
        val primary = t.substringBefore('-')
        if (primary in rtlLangs) return true
        // If any subtag explicitly specifies the Arabic script (e.g., ms-Arab)
        val parts = t.split('-')
        if (parts.any { it == "arab" }) return true
        return false
    }

    // Given a system or app locale, find the best supported language tag.
    // Strategy: exact match; then language-only match; then region-specific fallbacks for known pairs; else English.
    fun bestMatchFor(locale: Locale): String {
        val tag = locale.toLanguageTag()
        // Exact tag match
        if (all.any { it.tag.equals(tag, ignoreCase = true) }) return tag
        val lang = locale.language
        // Known mappings for Chinese
        if (lang.equals("zh", ignoreCase = true)) {
            val region = locale.country.uppercase(Locale.ROOT)
            return when (region) {
                "TW", "HK", "MO" -> all.find { it.tag == "zh-TW" }?.tag ?: "en"
                else -> all.find { it.tag == "zh-CN" }?.tag ?: "en"
            }
        }
        // Portuguese
        if (lang.equals("pt", ignoreCase = true)) {
            val region = locale.country.uppercase(Locale.ROOT)
            return when (region) {
                "BR" -> all.find { it.tag == "pt-BR" }?.tag ?: all.find { it.tag == "pt-PT" }?.tag ?: "en"
                else -> all.find { it.tag == "pt-PT" }?.tag ?: all.find { it.tag == "pt-BR" }?.tag ?: "en"
            }
        }
        // Malay explicit Arabic script mapping
        if (lang.equals("ms", ignoreCase = true)) {
            return all.find { it.tag == "ms" }?.tag ?: "en"
        }
        // General language-only match
        all.firstOrNull { it.tag.equals(lang, ignoreCase = true) }?.let { return it.tag }
        return "en"
    }
}