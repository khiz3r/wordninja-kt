# wordninja-kt

Kotlin port of the Python [wordninja](https://github.com/keredson/wordninja) library —
splits concatenated / camelCase strings into their constituent words using a
Viterbi dynamic-programming segmenter over a 126k frequency-ranked word list.

Fully usable from **Java** and **Kotlin**. Zero external runtime dependencies.

---

## What it does

```
"DataNameValue"          → [Data, Name, Value]
"variableisgoodperson"   → [variable, is, good, person]
"Prod2024AdminKey"       → [Prod, 2024, Admin, Key]
"xai-sdDAUQTf4W38P2..."  → [x, a, i, s, dDA, UQT, f, 4, W, 38, ...]  ← garbage fragments
```

The word-ratio of the first three is ≥ 0.6 → they look like variable names.
The API key produces mostly non-word fragments → ratio < 0.2 → flag it as a secret.

---

## Build

```bash
git clone https://github.com/khiz3r/wordninja-kt
cd wordninja-kt
gradlew build          # runs tests + produces build/libs/wordninja-kt-1.0.0-all.jar
```

The fat jar (`-all.jar`) bundles Kotlin stdlib and the word list — drop it anywhere
and use it with no extra dependencies.

---

## Usage

### Kotlin

```kotlin
import io.github.wordninja.WordNinja
import io.github.wordninja.splitWords
import io.github.wordninja.wordRatio
import io.github.wordninja.isNaturalLanguage

// String extensions (most idiomatic)
"DataNameValue".splitWords()           // [Data, Name, Value]
"Prod2024AdminKey".wordRatio()         // 0.75  → likely a variable name
"DataNameValue".isNaturalLanguage()   // true  → drop it (not a secret)
"xai-sdDAUQTf4W38".isNaturalLanguage() // false → keep it (looks like a secret)

// Shared singleton (lazily initialised, thread-safe)
val ninja = WordNinja.getInstance()
ninja.split("helloworld")

// Custom word list (advanced)
val custom = WordNinja("/my_custom_words.txt")
custom.split("someInput")
```

### Java

```java
import io.github.wordninja.WordNinja;
import java.util.List;

// Static helpers — simplest for Java callers
List<String> tokens  = WordNinja.splitStatic("DataNameValue");
double       ratio   = WordNinja.wordRatioStatic("Prod2024AdminKey");
boolean      natural = WordNinja.isNaturalLanguageStatic("variableIsGoodName");

// Or use the shared singleton explicitly
WordNinja ninja = WordNinja.getInstance();
List<String> tokens2 = ninja.split("helloworld");
boolean      drop    = ninja.isNaturalLanguage("thisIsAVariableForTest");
```

---

## API

### `WordNinja(wordListResource: String = "/wordninja_words.txt")`

Constructor. `wordListResource` is a classpath resource path.
Use the no-arg constructor (or `getInstance()`) for the bundled 126k word list.

---

### `split(input: String): List<String>`

Splits `input` into words. Preserves original casing. Whitespace / punctuation
tokens (spaces, hyphens, underscores…) are passed through unchanged between
word runs.

---

### `wordRatio(value: String, minTokenCount: Int = 3): Double`

Returns the fraction of tokens that are real dictionary words (alpha-only, length > 1).
Returns `0.0` when the token count is below `minTokenCount`.

| Value | Ratio |
|---|---|
| `DataNameValue` | 1.0 |
| `Prod2024AdminKey` | 0.75 |
| `sk_live_51Hh2K9J3kL8mN…` | ~0.1 |

---

### `isNaturalLanguage(value: String, threshold: Double = 0.6): Boolean`

Returns `true` when `wordRatio(value) >= threshold`.
Use this as the last filter in a secret-detection pipeline — if `true`, the
value is likely a variable name / human-readable identifier and should be dropped.

---

### Static / companion methods (Java-friendly)

| Method | Description |
|---|---|
| `WordNinja.getInstance()` | Shared singleton (lazy, thread-safe) |
| `WordNinja.splitStatic(input)` | Split via singleton |
| `WordNinja.wordRatioStatic(value)` | Word ratio via singleton |
| `WordNinja.isNaturalLanguageStatic(value)` | NL check via singleton |

---

### Kotlin string extensions

| Extension | Description |
|---|---|
| `String.splitWords()` | Alias for `WordNinja.splitStatic(this)` |
| `String.wordRatio()` | Alias for `WordNinja.wordRatioStatic(this)` |
| `String.isNaturalLanguage()` | Alias for `WordNinja.isNaturalLanguageStatic(this)` |

---

## Recommended filter order (secret scanner)

```kotlin
fun shouldKeepAsSecret(value: String): Boolean {
    if (matchesKnownPattern(value))  return true   // regex: base64, hex, sk_live_, xai-…
    if (shannonEntropy(value) > 3.5) return true   // high character entropy
    if (hasSecretStructure(value))   return true   // mixedCase + digits + length ≥ 12
    if (value.isNaturalLanguage())   return false  // looks like a variable → drop
    return true
}
```

---

## Standalone demo (Java)

A minimal `WordNinjaDemo.java` console app is included, which reads input from
the user and prints the split result, word ratio, and natural-language check.

```bash
# Compile (needs the fat jar on the classpath)
javac -cp build/libs/wordninja-kt-1.0.0-all.jar WordNinjaDemo.java

# Run (Linux/macOS uses ':' as the classpath separator, Windows uses ';')
java -cp build/libs/wordninja-kt-1.0.0-all.jar:. WordNinjaDemo
```

Type any string at the prompt to see it split; type `exit` to quit.

---

## Credits

Algorithm and word list by [keredson/wordninja](https://github.com/keredson/wordninja)
(MIT licence). Kotlin port by khiz3r.

---

## License

MIT
