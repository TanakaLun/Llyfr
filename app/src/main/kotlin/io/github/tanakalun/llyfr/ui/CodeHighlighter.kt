package io.github.tanakalun.llyfr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private object Colors {
    val keyword = Color(0xFF7C4DFF)
    val string = Color(0xFF00C853)
    val comment = Color(0xFF90A4AE)
    val number = Color(0xFFFF6D00)
    val function = Color(0xFF00B0FF)
    val type = Color(0xFFFFAB00)
    val annotation = Color(0xFFFF4081)
    val operator = Color(0xFFE040FB)
    val importStatement = Color(0xFF18FFFF)
    val constant = Color(0xFFFF6E40)
    val tag = Color(0xFF76FF03)
    val attribute = Color(0xFFFFD740)
    val punctuation = Color(0xFFB0BEC5)
}

private data class TokenPattern(
    val regex: Regex,
    val color: Color,
    val weight: FontWeight = FontWeight.Normal,
    val italic: Boolean = false,
)

private interface LanguageDef {
    val patterns: List<TokenPattern>
    val commentLine: String?
    val commentBlockStart: String?
    val commentBlockEnd: String?
}

private object LangKotlin : LanguageDef {
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val patterns = listOf(
        TokenPattern(Regex("""@"\w+"""), Colors.annotation),
        TokenPattern(Regex("""\b(package|import)\b"""), Colors.importStatement, FontWeight.Medium),
        TokenPattern(Regex("""\b(fun|val|var|class|interface|object|enum|sealed|data|abstract|open|private|protected|public|internal|override|suspend|inline|crossinline|reified|noinline|tailrec|operator|infix|extension|companion|init|get|set|by|lazy|lateinit|const|typealias|annotation|actual|expect|dynamic|outer|inner|inject|it|where|as|is|in|!in|!is|break|continue|return|throw|try|catch|finally|do|for|while|if|else|when|true|false|null|this|super)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b(Int|Long|Short|Byte|Float|Double|Char|Boolean|String|Unit|Nothing|Any|Array|List|MutableList|Map|MutableMap|Set|MutableSet|Pair|Triple|Sequence|Flow|StateFlow|SharedFlow|Context|Intent|View|Composable|Modifier|Color|TextStyle|FontWeight|Dp|Sp)\b"""), Colors.type),
        TokenPattern(Regex("""\b\d+\.?\d*[fFlLdD]?\b"""), Colors.number),
        TokenPattern(Regex("""("[^"\\]*(\\.[^"\\]*)*"|'[^'\\]*(\\.[^'\\]*)*')"""), Colors.string),
        TokenPattern(Regex("""`[^`]+`"""), Colors.function),
        TokenPattern(Regex("""\b([A-Z]\w*)\b(?=\s*[.(])"""), Colors.function),
        TokenPattern(Regex("""\b[A-Z][A-Z_0-9]+\b"""), Colors.constant),
        TokenPattern(Regex("""[+\-*/%=<>!&|^~?:]+"""), Colors.operator),
    )
}

private object LangJava : LanguageDef {
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val patterns = listOf(
        TokenPattern(Regex("""@\w+"""), Colors.annotation),
        TokenPattern(Regex("""\b(package|import)\b"""), Colors.importStatement, FontWeight.Medium),
        TokenPattern(Regex("""\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null|var|yield|record|sealed|permits)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b(int|long|short|byte|float|double|char|boolean|String|Integer|Long|Short|Byte|Float|Double|Character|Boolean|Object|Class|List|Map|Set|ArrayList|HashMap|HashSet|Optional|Stream|Consumer|Function|Supplier|Predicate|CompletableFuture)\b"""), Colors.type),
        TokenPattern(Regex("""\b\d+\.?\d*[fFlLdD]?\b"""), Colors.number),
        TokenPattern(Regex("""("[^"\\]*(\\.[^"\\]*)*")"""), Colors.string),
        TokenPattern(Regex("""\b([A-Z]\w*)\b(?=\s*[.(])"""), Colors.function),
        TokenPattern(Regex("""\b[A-Z][A-Z_0-9]+\b"""), Colors.constant),
        TokenPattern(Regex("""[+\-*/%=<>!&|^~?:]+"""), Colors.operator),
    )
}

private object LangPython : LanguageDef {
    override val commentLine = "#"
    override val commentBlockStart = null
    override val commentBlockEnd = null
    override val patterns = listOf(
        TokenPattern(Regex("""\b(from|import)\b"""), Colors.importStatement, FontWeight.Medium),
        TokenPattern(Regex("""\b(def|class|return|if|elif|else|for|while|break|continue|pass|raise|try|except|finally|with|as|lambda|yield|global|nonlocal|del|in|is|not|and|or|assert|async|await|True|False|None)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b(int|float|str|bool|list|dict|set|tuple|bytes|type|object|None|True|False|print|len|range|enumerate|zip|map|filter|sorted|reversed|isinstance|hasattr|getattr|setattr|super|property|staticmethod|classmethod)\b"""), Colors.type),
        TokenPattern(Regex("""\b\d+\.?\d*[jJ]?\b"""), Colors.number),
        TokenPattern(Regex("""(f?"[^"\\]*(\\.[^"\\]*)*"|f?'[^'\\]*(\\.[^'\\]*)*')"""), Colors.string),
        TokenPattern(Regex("""\b([a-z]\w*)\b(?=\s*\()"""), Colors.function),
        TokenPattern(Regex("""\b[A-Z][A-Z_0-9]+\b"""), Colors.constant),
        TokenPattern(Regex("""[+\-*/%=<>!&|^~@:]+"""), Colors.operator),
    )
}

private object LangJS : LanguageDef {
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val patterns = listOf(
        TokenPattern(Regex("""`[^`]*`"""), Colors.string),
        TokenPattern(Regex("""\b(from|import|export|as)\b"""), Colors.importStatement, FontWeight.Medium),
        TokenPattern(Regex("""\b(async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|false|finally|for|function|if|in|instanceof|let|new|null|of|return|static|super|switch|this|throw|true|try|typeof|undefined|var|void|while|with|yield)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b(number|string|boolean|any|void|never|unknown|object|symbol|bigint|undefined|null|Array|Promise|Map|Set|Record|Partial|Required|Readonly|Pick|Omit|React|Component|useState|useEffect|useRef|useMemo|useCallback)\b"""), Colors.type),
        TokenPattern(Regex("""\b\d+\.?\d*[eE]?[+-]?\d*[nN]?\b"""), Colors.number),
        TokenPattern(Regex("""('[^'\\]*(\\.[^'\\]*)*'|"[^"\\]*(\\.[^"\\]*)*")"""), Colors.string),
        TokenPattern(Regex("""\b([a-zA-Z_$]\w*)\b(?=\s*\()"""), Colors.function),
        TokenPattern(Regex("""\b[A-Z][A-Z_0-9]+\b"""), Colors.constant),
        TokenPattern(Regex("""[+\-*/%=<>!&|^~?:]+"""), Colors.operator),
    )
}

private object LangCSS : LanguageDef {
    override val commentLine = null
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val patterns = listOf(
        TokenPattern(Regex("""@media|@keyframes|@import|@font-face|@supports|@charset|@namespace|@page"""), Colors.annotation),
        TokenPattern(Regex("""#[a-zA-Z][\w-]*"""), Colors.type),
        TokenPattern(Regex("""\.[a-zA-Z][\w-]*"""), Colors.function),
        TokenPattern(Regex("""\b(important|inherit|initial|unset|revert|auto|none|normal|bold|italic|underline|solid|dashed|dotted|flex|grid|block|inline|inline-block|absolute|relative|fixed|sticky)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b(color|background|margin|padding|border|font|display|position|width|height|top|left|right|bottom|z-index|opacity|overflow|transition|animation|transform|justify|align|gap|grid|flex)\b"""), Colors.type),
        TokenPattern(Regex("""\b\d+\.?\d*(px|em|rem|%|vh|vw|vmin|vmax|deg|rad|ms|s)?\b"""), Colors.number),
        TokenPattern(Regex("""'[^']*'|"[^"]*"""), Colors.string),
        TokenPattern(Regex("""#[0-9a-fA-F]{3,8}\b"""), Colors.number),
        TokenPattern(Regex("""[:{};,]+"""), Colors.punctuation),
    )
}

private object LangHTML : LanguageDef {
    override val commentLine = null
    override val commentBlockStart = "<!--"
    override val commentBlockEnd = "-->"
    override val patterns = listOf(
        TokenPattern(Regex("""</?[a-zA-Z][\w-]*"""), Colors.tag),
        TokenPattern(Regex("""\b(class|id|style|href|src|alt|title|name|type|value|placeholder|action|method|rel|target|disabled|checked|selected|required|readonly|hidden|tabindex|aria-\w+|data-\w+)\b(?=\s*=)"""), Colors.attribute),
        TokenPattern(Regex(""""[^"]*"""") , Colors.string),
        TokenPattern(Regex("""'[^']*'"""), Colors.string),
        TokenPattern(Regex("""/>"""), Colors.punctuation),
    )
}

private object LangJSON : LanguageDef {
    override val commentLine = null
    override val commentBlockStart = null
    override val commentBlockEnd = null
    override val patterns = listOf(
        TokenPattern(Regex("""("[^"\\]*(\\.[^"\\]*)*")(?=\s*:)"""), Colors.type),
        TokenPattern(Regex("""("[^"\\]*(\\.[^"\\]*)*")"""), Colors.string),
        TokenPattern(Regex("""\b(true|false|null)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""-?\b\d+\.?\d*([eE][+-]?\d+)?\b"""), Colors.number),
        TokenPattern(Regex("""[{}\[\]:,]+"""), Colors.punctuation),
    )
}

private object LangYAML : LanguageDef {
    override val commentLine = "#"
    override val commentBlockStart = null
    override val commentBlockEnd = null
    override val patterns = listOf(
        TokenPattern(Regex("""^\s*[\w][\w .-]*(?=\s*:)""", RegexOption.MULTILINE), Colors.type),
        TokenPattern(Regex("""\b(true|false|yes|no|on|off|null|~)\b""", RegexOption.IGNORE_CASE), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b\d+\.?\d*\b"""), Colors.number),
        TokenPattern(Regex("""'[^']*'"""), Colors.string),
        TokenPattern(Regex("""#[^\n]*"""), Colors.comment, italic = true),
        TokenPattern(Regex("""[&*][\w]+"""), Colors.annotation),
        TokenPattern(Regex("""\b(null)\b"""), Colors.keyword),
        TokenPattern(Regex("""[|>-]+"""), Colors.operator),
        TokenPattern(Regex("""(:\s*|-\s*)"""), Colors.punctuation),
    )
}

private object LangPlain : LanguageDef {
    override val commentLine = null
    override val commentBlockStart = null
    override val commentBlockEnd = null
    override val patterns: List<TokenPattern> = emptyList()
}

private val languageMap = mapOf(
    "kotlin" to LangKotlin, "kt" to LangKotlin, "kts" to LangKotlin,
    "java" to LangJava,
    "python" to LangPython, "py" to LangPython,
    "javascript" to LangJS, "js" to LangJS, "jsx" to LangJS,
    "typescript" to LangTS, "ts" to LangTS, "tsx" to LangTS,
    "css" to LangCSS, "scss" to LangCSS,
    "html" to LangHTML, "xml" to LangHTML, "htm" to LangHTML,
    "json" to LangJSON,
    "yaml" to LangYAML, "yml" to LangYAML,
)

private object LangTS : LanguageDef {
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val patterns = listOf(
        TokenPattern(Regex("""`[^`]*`"""), Colors.string),
        TokenPattern(Regex("""\b(from|import|export|as|type|interface|declare|namespace|module)\b"""), Colors.importStatement, FontWeight.Medium),
        TokenPattern(Regex("""\b(async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|false|finally|for|function|if|in|instanceof|let|new|null|of|return|static|super|switch|this|throw|true|try|typeof|undefined|var|void|while|with|yield)\b"""), Colors.keyword, FontWeight.Medium),
        TokenPattern(Regex("""\b(number|string|boolean|any|void|never|unknown|object|symbol|bigint|undefined|null|Array|Promise|Map|Set|Record|Partial|Required|Readonly|Pick|Omit|React|Component|useState|useEffect|useRef|useMemo|useCallback)\b"""), Colors.type),
        TokenPattern(Regex("""\b\d+\.?\d*[eE]?[+-]?\d*[nN]?\b"""), Colors.number),
        TokenPattern(Regex("""('[^'\\]*(\\.[^'\\]*)*'|"[^"\\]*(\\.[^"\\]*)*")"""), Colors.string),
        TokenPattern(Regex("""\b([a-zA-Z_$]\w*)\b(?=\s*\()"""), Colors.function),
        TokenPattern(Regex("""\b[A-Z][A-Z_0-9]+\b"""), Colors.constant),
        TokenPattern(Regex("""[+\-*/%=<>!&|^~?:]+"""), Colors.operator),
    )
}

fun highlightCode(code: String, language: String): AnnotatedString {
    val lang = language.lowercase().trim()
    val def = languageMap[lang] ?: LangPlain

    if (def.patterns.isEmpty()) {
        return AnnotatedString(code)
    }

    val allPatterns = def.patterns

    return buildAnnotatedString {
        var pos = 0
        while (pos < code.length) {
            var bestMatch: MatchResult? = null
            var bestPattern: TokenPattern? = null
            var bestPriority = Int.MAX_VALUE

            for ((priority, pattern) in allPatterns.withIndex()) {
                val match = pattern.regex.find(code, pos)
                if (match != null && match.range.first == pos && priority < bestPriority) {
                    bestMatch = match
                    bestPattern = pattern
                    bestPriority = priority
                }
            }

            if (bestMatch != null && bestPattern != null) {
                val style = SpanStyle(
                    color = bestPattern.color,
                    fontWeight = bestPattern.weight,
                    fontStyle = if (bestPattern.italic) FontStyle.Italic else FontStyle.Normal,
                )
                withStyle(style) {
                    append(bestMatch.value)
                }
                pos = bestMatch.range.last + 1
            } else {
                append(code[pos])
                pos++
            }
        }
    }
}
