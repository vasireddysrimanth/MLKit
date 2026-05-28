package com.devsrimanth.mlkit.textRecognization

enum class RecognitionLanguage(val label: String, val flag: String) {
    LATIN("Latin / English", "🇬🇧"),
    CHINESE("Chinese (简体)", "🇨🇳"),
    DEVANAGARI("Devanagari (हिन्दी)", "🇮🇳"),
    JAPANESE("Japanese (日本語)", "🇯🇵"),
    KOREAN("Korean (한국어)", "🇰🇷")
}