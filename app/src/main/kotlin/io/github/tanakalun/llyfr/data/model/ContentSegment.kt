package io.github.tanakalun.llyfr.data.model

import java.util.UUID

internal val IMAGE_PATH_REGEX = Regex("""!\[.*?\]\((.*?)\)""")
private val FENCED_CODE_REGEX = Regex("""```(\w*)\n([\s\S]*?)```""")

sealed class ContentSegment {
    abstract val id: String

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val markdown: String = "",
    ) : ContentSegment()

    data class Image(
        override val id: String = UUID.randomUUID().toString(),
        val path: String,
        val width: Int = 0,
        val height: Int = 0,
    ) : ContentSegment()

    data class Code(
        override val id: String = UUID.randomUUID().toString(),
        val language: String = "",
        val code: String = "",
    ) : ContentSegment()
}

private class Marker(
    val start: Int,
    val end: Int,
    val segment: ContentSegment,
    val isImage: Boolean,
)

fun parseSegmentsFromMarkdown(markdown: String): List<ContentSegment> {
    if (markdown.isBlank()) return listOf(ContentSegment.Text())

    val codeRanges = mutableListOf<Pair<Int, Int>>()
    FENCED_CODE_REGEX.findAll(markdown).forEach { match ->
        codeRanges.add(match.range.first to match.range.last + 1)
    }

    val markers = mutableListOf<Marker>()
    FENCED_CODE_REGEX.findAll(markdown).forEach { match ->
        markers.add(
            Marker(
                start = match.range.first,
                end = match.range.last + 1,
                segment = ContentSegment.Code(
                    language = match.groupValues[1],
                    code = match.groupValues[2].removeSuffix("\n"),
                ),
                isImage = false,
            )
        )
    }
    IMAGE_PATH_REGEX.findAll(markdown).forEach { match ->
        val path = match.groupValues[1]
        if (path.startsWith("/") || path.startsWith("file:")) {
            val range = match.range.first to match.range.last + 1
            val insideCode = codeRanges.any { range.first >= it.first && range.first < it.second }
            if (!insideCode) {
                markers.add(
                    Marker(
                        start = range.first,
                        end = range.second,
                        segment = ContentSegment.Image(path = path),
                        isImage = true,
                    )
                )
            }
        }
    }
    markers.sortBy { it.start }

    val segments = mutableListOf<ContentSegment>()
    var lastEnd = 0
    var prevWasImage = false
    markers.forEach { marker ->
        var chunk = markdown.substring(lastEnd, marker.start)
        if (prevWasImage && chunk.startsWith("\n")) {
            chunk = chunk.substring(1)
        }
        if (marker.isImage && chunk.endsWith("\n")) {
            chunk = chunk.dropLast(1)
        }
        if (chunk.isNotBlank()) {
            segments.add(ContentSegment.Text(markdown = chunk))
        }
        segments.add(marker.segment)
        lastEnd = marker.end
        prevWasImage = marker.isImage
    }
    var tail = markdown.substring(lastEnd)
    if (prevWasImage && tail.startsWith("\n")) {
        tail = tail.substring(1)
    }
    if (tail.isNotBlank()) {
        segments.add(ContentSegment.Text(markdown = tail))
    }

    if (segments.isEmpty()) {
        segments.add(ContentSegment.Text())
    }
    if (segments.first() !is ContentSegment.Text) {
        segments.add(0, ContentSegment.Text())
    }
    if (segments.last() !is ContentSegment.Text) {
        segments.add(ContentSegment.Text())
    }

    return segments
}

fun serializeSegmentsToMarkdown(segments: List<ContentSegment>): String {
    val sb = StringBuilder()
    segments.forEach { segment ->
        when (segment) {
            is ContentSegment.Text -> {
                sb.append(segment.markdown)
            }
            is ContentSegment.Image -> {
                if (sb.isNotEmpty()) {
                    sb.append('\n')
                }
                sb.append("![image](${segment.path})")
                sb.append('\n')
            }
            is ContentSegment.Code -> {
                if (segment.language.isNotBlank()) {
                    sb.append("```${segment.language}\n${segment.code}\n```")
                } else {
                    sb.append("```\n${segment.code}\n```")
                }
            }
        }
    }
    return sb.toString()
}
