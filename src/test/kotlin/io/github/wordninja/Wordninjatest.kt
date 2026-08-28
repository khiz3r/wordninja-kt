package io.github.wordninja

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WordNinjaTest {

    private val ninja = WordNinja()

    // ── split() ───────────────────────────────────────────────────────────────

    @Test fun `splits clean camelCase variable names`() {
        assertEquals(listOf("Data", "Name", "Value"), ninja.split("DataNameValue"))
    }

    @Test fun `splits concatenated lowercase words`() {
        val result = ninja.split("variableisgoodperson")
        assertTrue(result.containsAll(listOf("variable", "is", "good", "person")))
    }

    @Test fun `splits camelCase with number`() {
        val result = ninja.split("Prod2024AdminKey")
        assertTrue(result.containsAll(listOf("Prod", "Admin", "Key")))
    }

    @Test fun `preserves original casing`() {
        val result = ninja.split("DataNameValue")
        assertEquals("Data", result[0])
        assertEquals("Name", result[1])
        assertEquals("Value", result[2])
    }

    @Test fun `handles empty string`() {
        assertEquals(emptyList<String>(), ninja.split(""))
    }

    @Test fun `produces many fragments for a real API key`() {
        val key = "xai-sdDAUQTf4W38P2l4aEIkS8PBs5GqX57EBjwAOer9w1OamYsPHSLRpsxD9rNirK3"
        val tokens = ninja.split(key.filter { it.isLetterOrDigit() })
        // real API key → lots of short garbage fragments, not a few clean words
        assertTrue(tokens.size > 15, "Expected many fragments, got ${tokens.size}: $tokens")
    }

    // ── wordRatio() ───────────────────────────────────────────────────────────

    @Test fun `word ratio is high for variable names`() {
        assertTrue(ninja.wordRatio("DataNameValue") >= 0.6)
        assertTrue(ninja.wordRatio("variableisgoodperson") >= 0.6)
        assertTrue(ninja.wordRatio("Prod2024AdminKey") >= 0.6)
    }

    @Test fun `word ratio is low for real secrets`() {
        // Stripe-style key body
        val key = "sk51Hh2K9J3kL8mN0pQrSt4UvWxYz"
        assertTrue(ninja.wordRatio(key) < 0.6)
    }

    @Test fun `word ratio returns 0 for very short input`() {
        assertEquals(0.0, ninja.wordRatio("ab"))          // < 3 tokens
        assertEquals(0.0, ninja.wordRatio("apiKey"))      // 2 tokens
    }

    // ── isNaturalLanguage() ───────────────────────────────────────────────────

    @Test fun `correctly labels variable names as natural language`() {
        assertTrue(ninja.isNaturalLanguage("DataNameValue"))
        assertTrue(ninja.isNaturalLanguage("variableisgoodperson"))
        assertTrue(ninja.isNaturalLanguage("thisIsAVariableForTest"))
    }

    @Test fun `correctly labels API keys as NOT natural language`() {
        assertFalse(ninja.isNaturalLanguage("xai-sdDAUQTf4W38P2l4aEIkS8PBs5GqX57"))
    }

    // ── Kotlin extensions ─────────────────────────────────────────────────────

    @Test fun `String splitWords extension works`() {
        assertEquals(listOf("Data", "Name", "Value"), "DataNameValue".splitWords())
    }

    @Test fun `String wordRatio extension works`() {
        assertTrue("DataNameValue".wordRatio() >= 0.6)
    }

    @Test fun `String isNaturalLanguage extension works`() {
        assertTrue("DataNameValue".isNaturalLanguage())
        assertFalse("xai-sdDAUQTf4W38P2l4".isNaturalLanguage())
    }

    // ── Static Java-friendly API ──────────────────────────────────────────────

    @Test fun `static splitStatic works`() {
        assertTrue(WordNinja.splitStatic("helloworld").isNotEmpty())
    }

    @Test fun `static wordRatioStatic works`() {
        assertTrue(WordNinja.wordRatioStatic("DataNameValue") >= 0.6)
    }

    @Test fun `static isNaturalLanguageStatic works`() {
        assertTrue(WordNinja.isNaturalLanguageStatic("DataNameValue"))
    }

    @Test fun `singleton instance is reused`() {
        assertSame(WordNinja.getInstance(), WordNinja.getInstance())
    }
}