package io.github.wordninja

/**
 * Top-level Kotlin convenience extensions.
 *
 * These let you call wordninja directly on any String without
 * managing a [WordNinja] instance:
 *
 * ```kotlin
 * "DataNameValue".splitWords()          // ["Data", "Name", "Value"]
 * "sk_live_abc123XYZ".wordRatio()       // ~0.1  → likely a secret
 * "variableIsGoodName".isNaturalLanguage() // true → drop it
 * ```
 */

/** Splits this string into words using the shared [WordNinja] instance. */
fun String.splitWords(): List<String> = WordNinja.splitStatic(this)

/**
 * Returns the fraction of tokens that are real dictionary words.
 * Higher = more "natural language". Values ≥ 0.6 are typically variable names.
 *
 * @param minTokenCount  skip scoring if fewer tokens than this (returns 0.0).
 */
fun String.wordRatio(minTokenCount: Int = 3): Double =
    WordNinja.wordRatioStatic(this, minTokenCount)

/**
 * Returns `true` when this string looks like a variable name / natural language
 * rather than a secret or random token.
 *
 * @param threshold  word-ratio threshold (default 0.6).
 */
fun String.isNaturalLanguage(threshold: Double = 0.6): Boolean =
    WordNinja.isNaturalLanguageStatic(this, threshold)