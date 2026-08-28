package io.github.wordninja

import kotlin.math.ln

/**
 * WordNinja — Kotlin port of the Python wordninja library.
 *
 * Splits concatenated / camelCase strings into their constituent words using
 * a Viterbi / dynamic-programming segmenter over a frequency-ranked word list
 * (Zipf's law cost model, same as the original Python implementation).
 *
 * Fully usable from Java:
 * ```java
 * import io.github.wordninja.WordNinja;
 * List<String> tokens = WordNinja.getInstance().split("helloworld");
 * // or via the static helper:
 * List<String> tokens = WordNinja.splitStatic("helloworld");
 * ```
 *
 * Kotlin usage:
 * ```kotlin
 * val ninja = WordNinja()          // custom word list
 * val tokens = ninja.split("DataNameValue")   // ["Data","Name","Value"]
 * // or the top-level extension / companion shorthand:
 * val tokens = WordNinja.splitStatic("sk_live_abc123")
 * ```
 *
 * Thread-safe: [wordCost] and [maxWordLen] are read-only after construction.
 *
 * @param wordListResource  classpath resource path to the newline-separated,
 *                          frequency-ranked word list.  Defaults to the bundled
 *                          126 k-word list derived from wordninja_words.txt.
 */
class WordNinja @JvmOverloads constructor(
    wordListResource: String = DEFAULT_WORD_LIST
) {

    /** Maps each word (lower-cased) to its Zipf log-cost. */
    private val wordCost: Map<String, Double>

    /** Length of the longest word in the list — caps the DP look-back window. */
    private val maxWordLen: Int

    init {
        val stream = WordNinja::class.java
            .getResourceAsStream(wordListResource)
            ?: error("Word list resource not found: $wordListResource")

        val words = stream.bufferedReader(Charsets.UTF_8).readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val logN = ln(words.size.toDouble())
        wordCost = HashMap<String, Double>(words.size * 2).also { map ->
            words.forEachIndexed { index, word ->
                map[word.lowercase()] = ln((index + 1) * logN)
            }
        }
        maxWordLen = words.maxOf { it.length }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Splits [input] into words.
     *
     * Punctuation / whitespace tokens are preserved as-is between word runs,
     * matching the Python behaviour (spaces, hyphens, underscores etc. act as
     * natural split points and are returned unchanged in the token list).
     *
     * @param input  any string — camelCase, snake_case, concatenated words, etc.
     * @return       ordered list of tokens (original casing preserved).
     */
    fun split(input: String): List<String> {
        if (input.isEmpty()) return emptyList()

        // Split on whitespace / punctuation boundaries, keeping the delimiters
        val parts = SPLIT_RE.split(input)
        val delimiters = SPLIT_RE.findAll(input).map { it.value }.toList()

        val result = mutableListOf<String>()
        parts.forEachIndexed { idx, text ->
            result.addAll(splitWord(text))
            if (idx < delimiters.size) result.add(delimiters[idx])
        }
        return result
    }

    /**
     * Computes the fraction of tokens (from [split]) that are real dictionary
     * words (alpha-only, length > 1).  Useful for deciding whether a value
     * looks like a human-readable identifier vs a secret / random string.
     *
     * @param value          the string to evaluate.
     * @param minTokenCount  minimum number of tokens required before scoring
     *                       (returns 0.0 for very short inputs).  Default 3.
     * @return               ratio in [0.0, 1.0]; higher means more "natural language".
     */
    @JvmOverloads
    fun wordRatio(value: String, minTokenCount: Int = 3): Double {
        val clean = value.filter { it.isLetterOrDigit() }
        val tokens = split(clean)
        if (tokens.size < minTokenCount) return 0.0
        val realWords = tokens.count { token ->
            token.all { it.isLetter() } &&
                token.length > 1 &&
                token.lowercase() in wordCost
        }
        return realWords.toDouble() / tokens.size
    }

    /**
     * Returns `true` when [value] looks like natural language / a variable
     * name rather than a secret.  Uses [wordRatio] with the supplied threshold.
     *
     * @param value      the string to evaluate.
     * @param threshold  word-ratio above which the value is considered natural
     *                   language.  Default 0.6 (60 %).
     */
    @JvmOverloads
    fun isNaturalLanguage(value: String, threshold: Double = 0.6): Boolean =
        wordRatio(value) >= threshold

    // ── Internal DP segmenter ─────────────────────────────────────────────────

    /**
     * Core Viterbi segmenter — direct port of the Python `_split` method.
     * Preserves original character case in the returned tokens.
     */
    private fun splitWord(s: String): List<String> {
        if (s.isEmpty()) return emptyList()

        val n = s.length
        // cost[i] = minimum accumulated cost to segment s[0..i)
        val cost = DoubleArray(n + 1) { if (it == 0) 0.0 else Double.MAX_VALUE }
        // split[i] = token length k such that s[i-k..i) is the last token
        val splitAt = IntArray(n + 1)

        for (i in 1..n) {
            var bestCost = Double.MAX_VALUE
            var bestK = 1
            val start = maxOf(0, i - maxWordLen)
            for (k in 1..(i - start)) {
                val j = i - k
                val prev = cost[j]
                if (prev == Double.MAX_VALUE) continue
                val word = s.substring(j, i).lowercase()
                val wc = wordCost[word] ?: UNKNOWN_WORD_COST
                val c = prev + wc
                if (c < bestCost) {
                    bestCost = c
                    bestK = k
                }
            }
            cost[i] = bestCost
            splitAt[i] = bestK
        }

        // Backtrack — collect tokens in reverse, then reverse the list
        val out = mutableListOf<String>()
        var i = n
        while (i > 0) {
            val k = splitAt[i]
            val token = s.substring(i - k, i)

            // Replicate Python apostrophe + digit-run merging
            if (out.isNotEmpty()) {
                when {
                    out.last() == "'s" -> {
                        out[out.lastIndex] = token + out.last()
                        i -= k
                        continue
                    }
                    s[i - 1].isDigit() && out.last().first().isDigit() -> {
                        out[out.lastIndex] = token + out.last()
                        i -= k
                        continue
                    }
                }
            }

            out.add(token)
            i -= k
        }

        out.reverse()
        return out
    }

    // ── Companion / static helpers ────────────────────────────────────────────

    companion object {
        private const val DEFAULT_WORD_LIST = "/wordninja_words.txt"

        /**
         * Cost assigned to words not found in the dictionary.
         * 9e999 → effectively infinite, forcing the DP to prefer known words.
         */
        private const val UNKNOWN_WORD_COST = 9e18

        /** Regex used to split on whitespace, keeping delimiters. */
        private val SPLIT_RE = Regex("""\s+""")

        // Lazily initialised singleton backed by the bundled word list
        @Volatile private var _instance: WordNinja? = null

        /**
         * Returns the shared [WordNinja] instance backed by the bundled word list.
         * Thread-safe (double-checked locking).
         *
         * Prefer this over constructing a new instance for every call —
         * construction reads ~126 k words from disk.
         */
        @JvmStatic
        fun getInstance(): WordNinja =
            _instance ?: synchronized(this) {
                _instance ?: WordNinja().also { _instance = it }
            }

        /**
         * Static shorthand — splits [input] using the shared instance.
         * Convenient for Java callers who don't want to manage the instance.
         *
         * ```java
         * List<String> tokens = WordNinja.splitStatic("helloworld");
         * ```
         */
        @JvmStatic
        fun splitStatic(input: String): List<String> = getInstance().split(input)

        /**
         * Static shorthand — word ratio using the shared instance.
         */
        @JvmStatic
        @JvmOverloads
        fun wordRatioStatic(value: String, minTokenCount: Int = 3): Double =
            getInstance().wordRatio(value, minTokenCount)

        /**
         * Static shorthand — natural language check using the shared instance.
         */
        @JvmStatic
        @JvmOverloads
        fun isNaturalLanguageStatic(value: String, threshold: Double = 0.6): Boolean =
            getInstance().isNaturalLanguage(value, threshold)
    }
}