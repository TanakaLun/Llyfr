package io.github.tanakalun.llyfr.util

fun stripMarkdown(text: String): String {
    if (text.isBlank()) return text
    return text
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
        .replace(Regex("_{1,3}([^_]+)_{1,3}"), "$1")
        .replace(Regex("~~([^~]+)~~"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("^\\s*[-*+]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*>\\s?", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*-\\s+\\[([ x])\\]\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("!\\[([^]]*)]\\([^)]+\\)"), "$1")
        .replace(Regex("```[\\s\\S]*?```"), "[code]")
        .replace(Regex("^---+$", RegexOption.MULTILINE), "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
