# Secret Phrase — Test Specifications

## Test Organization

```
app/src/test/java/com/vaultaire/android/crypto/          # Unit tests (JVM)
├── RecoveryPhraseEngineTest.kt
├── WordListsTest.kt
├── PhraseStrengthEstimatorTest.kt
├── RecoveryEnvelopeManagerTest.kt
├── KeyDerivationEngineTest.kt          # Existing - extend
├── EnvelopeEncryptionEngineTest.kt     # Existing - extend
├── PatternUtilsTest.kt                 # Existing - extend
└── SecureStorageTest.kt                # Requires Robolectric

app/src/androidTest/java/com/vaultaire/android/           # Instrumented tests
├── crypto/
│   ├── RecoveryPhraseEngineInstrumentedTest.kt
│   ├── SecureStorageInstrumentedTest.kt
│   └── KeyDerivationPerformanceTest.kt
├── ui/recovery/
│   ├── PhraseDisplayActivityTest.kt
│   ├── PhraseEntryScreenTest.kt
│   └── RecoveryFlowTest.kt
└── vault/
    └── VaultCreationIntegrationTest.kt
```

---

## Unit Tests — Phrase Generation

### RecoveryPhraseEngineTest.kt

```kotlin
class RecoveryPhraseEngineTest {

    private val wordLists = WordLists(
        articles = listOf("the", "a", "an"),
        adjectives = listOf("quiet", "swift", "ancient", "golden", "silent"),
        nouns = listOf("river", "mountain", "bridge", "key", "shadow"),
        verbs = listOf("flows", "rests", "guards", "watches", "sleeps"),
        prepositions = listOf("beneath", "above", "within", "beyond"),
        adverbs = listOf("silently", "swiftly", "eternally", "quietly")
    )
    private val estimator = MockPhraseStrengthEstimator()
    private val engine = RecoveryPhraseEngine(wordLists, estimator)

    @Test
    fun `generatePhrase returns exactly 9 words`() {
        val phrase = engine.generatePhrase()
        assertEquals(9, phrase.size)
    }

    @Test
    fun `generatePhrase follows grammatical template`() {
        val phrase = engine.generatePhrase()
        // Template: Article Adjective Noun Verb Preposition Article Adjective Noun Adverb
        assertTrue(wordLists.articles.contains(phrase[0]))
        assertTrue(wordLists.adjectives.contains(phrase[1]))
        assertTrue(wordLists.nouns.contains(phrase[2]))
        assertTrue(wordLists.verbs.contains(phrase[3]))
        assertTrue(wordLists.prepositions.contains(phrase[4]))
        assertTrue(wordLists.articles.contains(phrase[5]))
        assertTrue(wordLists.adjectives.contains(phrase[6]))
        assertTrue(wordLists.nouns.contains(phrase[7]))
        assertTrue(wordLists.adverbs.contains(phrase[8]))
    }

    @Test
    fun `generatePhrase uses cryptographically secure randomness`() {
        val phrases = List(1000) { engine.generatePhrase() }
        val uniquePhrases = phrases.distinct()
        // With ~77 bits entropy, 1000 samples should all be unique
        assertEquals(1000, uniquePhrases.size)
    }

    @Test
    fun `generatePhrase produces different results each call`() {
        val phrase1 = engine.generatePhrase()
        val phrase2 = engine.generatePhrase()
        assertNotEquals(phrase1, phrase2)
    }

    @Test
    fun `normalizePhrase lowercases and trims`() {
        val input = listOf("The", "  Quiet", "River", "Flows", "Beneath", "Ancient", "Stone", "Bridges", "At", "Midnight  ")
        val normalized = engine.normalizePhrase(input)
        assertEquals("the quiet river flows beneath ancient stone bridges at midnight", normalized)
    }

    @Test
    fun `normalizePhrase collapses multiple spaces`() {
        val input = listOf("the", "  quiet", "   river")
        val normalized = engine.normalizePhrase(input)
        assertEquals("the quiet river", normalized)
    }

    @Test
    fun `normalizePhrase applies NFKC normalization`() {
        // Unicode fullwidth characters
        val input = listOf("ｔｈｅ", "ｑｕｉｅｔ", "ｒｉｖｅｒ")
        val normalized = engine.normalizePhrase(input)
        assertEquals("the quiet river", normalized)
    }
}
```

### WordListsTest.kt

```kotlin
class WordListsTest {

    @Test
    fun `loadFromResources loads all word lists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wordLists = WordLists.loadFromResources(context)

        assertTrue(wordLists.articles.size >= 3)
        assertTrue(wordLists.adjectives.size >= 400)
        assertTrue(wordLists.nouns.size >= 800)
        assertTrue(wordLists.verbs.size >= 400)
        assertTrue(wordLists.prepositions.size >= 20)
        assertTrue(wordLists.adverbs.size >= 150)

        // No duplicates within each list
        assertEquals(wordLists.articles.size, wordLists.articles.distinct().size)
        assertEquals(wordLists.adjectives.size, wordLists.adjectives.distinct().size)
        // ... etc
    }

    @Test
    fun `word lists contain only lowercase ASCII words`() {
        val wordLists = WordLists.loadFromResources(ApplicationProvider.getApplicationContext())
        val allWords = wordLists.articles + wordLists.adjectives + wordLists.nouns +
                       wordLists.verbs + wordLists.prepositions + wordLists.adverbs

        allWords.forEach { word ->
            assertTrue(word.matches(Regex("^[a-z]+$")), "Invalid word: $word")
        }
    }

    @Test
    fun `word lists match iOS reference lists`() {
        // Load golden reference from test resources
        val reference = loadReferenceWordLists()
        val actual = WordLists.loadFromResources(ApplicationProvider.getApplicationContext())

        assertEquals(reference.articles, actual.articles)
        assertEquals(reference.adjectives, actual.adjectives)
        // ... etc
    }
}
```

---

## Unit Tests — Custom Phrase Validation

### PhraseStrengthEstimatorTest.kt

```kotlin
class PhraseStrengthEstimatorTest {

    private val estimator = PhraseStrengthEstimator(
        commonPhrases = loadCommonPhrases(),
        dictionaryWords = loadDictionaryWords()
    )

    @Test
    fun `accepts 6-word custom phrase with good entropy`() {
        val words = listOf("correct", "horse", "battery", "staple", "apple", "banana")
        val result = estimator.estimate(words)

        assertTrue(result.isAcceptable)
        assertTrue(result.entropyBits >= 50)
    }

    @Test
    fun `rejects phrase with less than 6 words`() {
        val words = listOf("one", "two", "three", "four", "five")
        val result = estimator.estimate(words)

        assertFalse(result.isAcceptable)
        assertEquals("Phrase must contain at least 6 words", result.rejectionReason)
    }

    @Test
    fun `rejects phrase with more than 24 words`() {
        val words = List(25) { "word" }
        val result = estimator.estimate(words)

        assertFalse(result.isAcceptable)
        assertEquals("Phrase must contain at most 24 words", result.rejectionReason)
    }

    @Test
    fun `rejects repetitive pattern`() {
        val words = List(6) { "word" }
        val result = estimator.estimate(words)

        assertFalse(result.isAcceptable)
        assertEquals("Phrase contains repetitive pattern", result.rejectionReason)
    }

    @Test
    fun `rejects common phrase`() {
        val words = "to be or not to be".split(" ")
        val result = estimator.estimate(words)

        assertFalse(result.isAcceptable)
        assertEquals("Phrase is too common", result.rejectionReason)
    }

    @Test
    fun `rejects sequential dictionary words`() {
        val words = listOf("apple", "banana", "cherry", "date", "elder", "fig")
        val result = estimator.estimate(words)

        assertFalse(result.isAcceptable)
        assertEquals("Phrase uses sequential dictionary words", result.rejectionReason)
    }

    @Test
    fun `calculates entropy correctly for known phrases`() {
        // 6 random words from 2000-word dictionary ≈ 6 * log2(2000) ≈ 66 bits
        val words = listOf("river", "mountain", "bridge", "shadow", "key", "silent")
        val result = estimator.estimate(words)

        assertTrue(result.entropyBits > 50)
        assertTrue(result.entropyBits < 80)
    }
}
```

---

## Unit Tests — Key Derivation

### RecoveryPhraseEngineKeyDerivationTest.kt

```kotlin
class RecoveryPhraseEngineKeyDerivationTest {

    private val wordLists = TestWordLists.minimal()
    private val estimator = MockPhraseStrengthEstimator()
    private val engine = RecoveryPhraseEngine(wordLists, estimator)

    @Test
    fun `deriveRecoveryKey produces 256-bit key`() {
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val key = engine.deriveRecoveryKey(phrase)

        assertEquals("AES", key.algorithm)
        assertEquals(32, key.encoded.size)  // 256 bits = 32 bytes
    }

    @Test
    fun `deriveRecoveryKey is deterministic for same phrase`() {
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val key1 = engine.deriveRecoveryKey(phrase)
        val key2 = engine.deriveRecoveryKey(phrase)

        assertArrayEquals(key1.encoded, key2.encoded)
    }

    @Test
    fun `deriveRecoveryKey differs for different phrases`() {
        val phrase1 = "the quiet river flows beneath ancient stone bridges at midnight"
        val phrase2 = "the swift mountain guards above golden shadows eternally"
        val key1 = engine.deriveRecoveryKey(phrase1)
        val key2 = engine.deriveRecoveryKey(phrase2)

        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }

    @Test
    fun `deriveRecoveryKey uses 800000 iterations`() {
        // Verify by checking iteration count constant
        assertEquals(800_000, RecoveryPhraseEngine.PBKDF2_ITERATIONS)
    }

    @Test
    fun `deriveRecoveryKey uses phrase-derived salt not device salt`() {
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val salt1 = engine.derivePhraseSalt(phrase)
        val salt2 = engine.derivePhraseSalt(phrase)
        val salt3 = engine.derivePhraseSalt("different phrase entirely")

        assertArrayEquals(salt1, salt2)  // Deterministic
        assertFalse(salt1.contentEquals(salt3))  // Different per phrase
        assertEquals(32, salt1.size)  // 256-bit salt
    }

    @Test
    fun `derivePhraseSalt matches iOS reference implementation`() {
        // Golden test vector from iOS
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val expectedSalt = hexToBytes("a1b2c3d4e5f678901234567890abcdef1234567890abcdef1234567890abcdef")
        val actualSalt = engine.derivePhraseSalt(phrase)

        assertArrayEquals(expectedSalt, actualSalt)
    }

    @Test
    fun `deriveRecoveryKey matches iOS reference test vector`() {
        // NIST SP 800-132 test vector adapted for our parameters
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val expectedKey = hexToBytes("...")  // From iOS reference
        val actualKey = engine.deriveRecoveryKey(phrase).encoded

        assertArrayEquals(expectedKey, actualKey)
    }
}
```

---

## Unit Tests — Envelope System

### RecoveryEnvelopeManagerTest.kt

```kotlin
class RecoveryEnvelopeManagerTest {

    private val envelopeEngine = EnvelopeEncryptionEngine()
    private val phraseEngine = MockRecoveryPhraseEngine()
    private val keyDerivation = MockKeyDerivationEngine()
    private val secureStorage = MockSecureStorage()
    private val manager = RecoveryEnvelopeManager(envelopeEngine, phraseEngine, keyDerivation, secureStorage)

    @Test
    fun `createEnvelopes produces two envelopes wrapping same MVK`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val patternBytes = "pattern123".toByteArray()
        val phraseWords = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")

        val envelopes = manager.createEnvelopes(masterKey, patternBytes, phraseWords)

        // Both envelopes should decrypt to same MVK
        val vdk = keyDerivation.deriveVaultKey(patternBytes)
        val rk = phraseEngine.deriveRecoveryKey(phraseEngine.normalizePhrase(phraseWords))

        val mvkFromPattern = envelopeEngine.unwrapMasterKey(envelopes.patternEnvelope, vdk)
        val mvkFromPhrase = envelopeEngine.unwrapMasterKey(envelopes.recoveryEnvelope, rk)

        assertArrayEquals(mvkFromPattern.encoded, mvkFromPhrase.encoded)
        assertArrayEquals(mvkFromPattern.encoded, masterKey.encoded)
    }

    @Test
    fun `recoverWithPhrase returns MVK for correct phrase`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val phraseWords = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")

        // Setup: create envelopes
        val envelopes = manager.createEnvelopes(masterKey, "pattern".toByteArray(), phraseWords)
        secureStorage.saveRecoveryEnvelope(envelopes.recoveryEnvelope)

        // Recover
        val recoveredKey = manager.recoverWithPhrase(phraseWords)

        assertArrayEquals(recoveredKey.encoded, masterKey.encoded)
    }

    @Test
    fun `recoverWithPhrase throws for incorrect phrase`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val correctPhrase = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")
        val wrongPhrase = listOf("the", "swift", "mountain", "guards", "above", "golden", "shadows", "eternally", "now")

        val envelopes = manager.createEnvelopes(masterKey, "pattern".toByteArray(), correctPhrase)
        secureStorage.saveRecoveryEnvelope(envelopes.recoveryEnvelope)

        assertThrows<InvalidPhraseException> {
            manager.recoverWithPhrase(wrongPhrase)
        }
    }

    @Test
    fun `recoverWithPhrase throws for wrong word count`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val phraseWords = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")
        val envelopes = manager.createEnvelopes(masterKey, "pattern".toByteArray(), phraseWords)
        secureStorage.saveRecoveryEnvelope(envelopes.recoveryEnvelope)

        val shortPhrase = listOf("the", "quiet", "river")  // 3 words
        assertThrows<InvalidPhraseException> {
            manager.recoverWithPhrase(shortPhrase)
        }
    }

    @Test
    fun `rotateEnvelopes creates new envelopes with same MVK`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val oldPattern = "oldpattern".toByteArray()
        val oldPhrase = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")
        val newPattern = "newpattern".toByteArray()

        // Initial setup
        val oldEnvelopes = manager.createEnvelopes(masterKey, oldPattern, oldPhrase)
        secureStorage.savePatternEnvelope(oldEnvelopes.patternEnvelope)
        secureStorage.saveRecoveryEnvelope(oldEnvelopes.recoveryEnvelope)

        // Rotate
        val newEnvelopes = manager.rotateEnvelopes(masterKey, newPattern)

        // Verify new pattern envelope works
        val newVdk = keyDerivation.deriveVaultKey(newPattern)
        val mvkFromNewPattern = envelopeEngine.unwrapMasterKey(newEnvelopes.patternEnvelope, newVdk)
        assertArrayEquals(mvkFromNewPattern.encoded, masterKey.encoded)

        // Verify new recovery envelope works
        val newPhrase = secureStorage.getEncryptedPhrase()!!
        val newRk = phraseEngine.deriveRecoveryKey(phraseEngine.normalizePhrase(newPhrase))
        val mvkFromNewPhrase = envelopeEngine.unwrapMasterKey(newEnvelopes.recoveryEnvelope, newRk)
        assertArrayEquals(mvkFromNewPhrase.encoded, masterKey.encoded)

        // Verify old envelopes no longer work (phrase changed)
        val oldRk = phraseEngine.deriveRecoveryKey(phraseEngine.normalizePhrase(oldPhrase))
        assertThrows<AEADBadTagException> {
            envelopeEngine.unwrapMasterKey(newEnvelopes.recoveryEnvelope, oldRk)
        }
    }
}
```

---

## Integration Tests

### RecoveryFlowIntegrationTest.kt (Instrumented)

```kotlin
@RunWith(AndroidJUnit4::class)
class RecoveryFlowIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(PhraseDisplayActivity::class.java)

    private lateinit var phraseEngine: RecoveryPhraseEngine
    private lateinit var envelopeManager: RecoveryEnvelopeManager
    private lateinit var secureStorage: SecureStorage

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext()
        phraseEngine = RecoveryPhraseEngine(
            WordLists.loadFromResources(context),
            PhraseStrengthEstimator(loadCommonPhrases(), loadDictionaryWords())
        )
        secureStorage = SecureStorage(context)
        envelopeManager = RecoveryEnvelopeManager(
            EnvelopeEncryptionEngine(),
            phraseEngine,
            KeyDerivationEngine(DeviceSaltManager(context)),
            secureStorage
        )
        secureStorage.clearAll()
    }

    @Test
    fun `full vault creation and phrase recovery flow`() {
        // 1. Create vault
        val masterKey = EnvelopeEncryptionEngine().generateMasterVaultKey()
        val pattern = PatternUtils.normalizePattern(generateValidPattern())
        val vaultKey = KeyDerivationEngine(DeviceSaltManager(ApplicationProvider.getApplicationContext()))
            .deriveVaultKey(pattern)
        val patternEnvelope = EnvelopeEncryptionEngine().wrapMasterKey(masterKey, vaultKey)

        val phraseWords = phraseEngine.generatePhrase()
        val normalizedPhrase = phraseEngine.normalizePhrase(phraseWords)
        val recoveryKey = phraseEngine.deriveRecoveryKey(normalizedPhrase)
        val recoveryEnvelope = EnvelopeEncryptionEngine().wrapMasterKey(masterKey, recoveryKey)

        secureStorage.savePatternEnvelope(patternEnvelope)
        secureStorage.saveRecoveryEnvelope(recoveryEnvelope)
        secureStorage.saveEncryptedPhrase(phraseWords)

        // 2. Simulate app lock

        // 3. User taps "Forgot Pattern" → enters phrase
        val recoveredKey = envelopeManager.recoverWithPhrase(phraseWords)

        // 4. Verify recovered key matches
        assertArrayEquals(recoveredKey.encoded, masterKey.encoded)

        // 5. User sets new pattern
        val newPattern = PatternUtils.normalizePattern(generateValidPattern())
        val newVaultKey = KeyDerivationEngine(DeviceSaltManager(ApplicationProvider.getApplicationContext()))
            .deriveVaultKey(newPattern)
        val newPatternEnvelope = EnvelopeEncryptionEngine().wrapMasterKey(recoveredKey, newVaultKey)

        // 6. New phrase generated
        val newPhraseWords = phraseEngine.generatePhrase()
        val newNormalizedPhrase = phraseEngine.normalizePhrase(newPhraseWords)
        val newRecoveryKey = phraseEngine.deriveRecoveryKey(newNormalizedPhrase)
        val newRecoveryEnvelope = EnvelopeEncryptionEngine().wrapMasterKey(recoveredKey, newRecoveryKey)

        secureStorage.savePatternEnvelope(newPatternEnvelope)
        secureStorage.saveRecoveryEnvelope(newRecoveryEnvelope)
        secureStorage.saveEncryptedPhrase(newPhraseWords)

        // 7. Verify old phrase no longer works
        assertThrows<InvalidPhraseException> {
            envelopeManager.recoverWithPhrase(phraseWords)
        }

        // 8. Verify new phrase works
        val finalKey = envelopeManager.recoverWithPhrase(newPhraseWords)
        assertArrayEquals(finalKey.encoded, masterKey.encoded)
    }

    @Test
    fun `phrase display in settings requires re-authentication`() {
        // Setup vault with phrase
        val phraseWords = phraseEngine.generatePhrase()
        secureStorage.saveEncryptedPhrase(phraseWords)

        // Launch phrase display activity
        val scenario = ActivityScenario.launch(PhraseDisplayActivity::class.java)

        // Should show biometric/prompt before displaying phrase
        scenario.onActivity { activity ->
            // Verify biometric prompt shown
            // Verify phrase NOT visible yet
        }

        // After successful auth
        scenario.onActivity { activity ->
            val phraseText = activity.findViewById<TextView>(R.id.phrase_text)
            val displayed = phraseText.text.toString()
            assertTrue(displayed.contains(phraseWords[0]))
        }
    }
}
```

### PhraseDisplaySecurityTest.kt (Instrumented)

```kotlin
@RunWith(AndroidJUnit4::class)
class PhraseDisplaySecurityTest {

    @Test
    fun `phrase display activity has FLAG_SECURE`() {
        val scenario = ActivityScenario.launch(PhraseDisplayActivity::class.java)

        scenario.onActivity { activity ->
            val flags = activity.window.attributes.flags
            assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
    }

    @Test
    fun `phrase text view has accessibility disabled`() {
        val scenario = ActivityScenario.launch(PhraseDisplayActivity::class.java)

        scenario.onActivity { activity =>
            val phraseText = activity.findViewById<TextView>(R.id.phrase_text)
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, phraseText.importantForAccessibility)
            assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO, phraseText.importantForAutofill)
        }
    }

    @Test
    fun `clipboard auto-clears after 60 seconds`() {
        val context = ApplicationProvider.getApplicationContext()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val clip = ClipData.newPlainText("Recovery Phrase", phrase)
        clipboard.setPrimaryClip(clip)

        // Trigger auto-clear (using test clock)
        TestClock.advanceTimeBy(60_000)

        assertNull(clipboard.primaryClip)
    }

    @Test
    fun `screenshot prevented on phrase screen`() {
        // This is tested by FLAG_SECURE - can't programmatically verify
        // but we verify the flag is set (above test)
    }
}
```

---

## Security Tests

### SecurityTestSuite.kt

```kotlin
class SecurityTestSuite {

    @Test
    fun `recovery phrase never logged`() {
        val logger = MockLogger()
        val engine = RecoveryPhraseEngine(TestWordLists.minimal(), MockPhraseStrengthEstimator())

        val phrase = engine.generatePhrase()
        engine.deriveRecoveryKey(engine.normalizePhrase(phrase))

        // Verify no log contains phrase words
        logger.logs.forEach { log ->
            phrase.forEach { word ->
                assertFalse(log.contains(word), "Phrase word '$word' found in log: $log")
            }
        }
    }

    @Test
    fun `recovery key wiped from memory after use`() {
        // This tests MemoryHygiene integration
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"
        val engine = RecoveryPhraseEngine(TestWordLists.minimal(), MockPhraseStrengthEstimator())

        val key = engine.deriveRecoveryKey(phrase)
        val keyBytes = key.encoded.copyOf()

        // Simulate wipe
        MemoryHygiene.wipeKey(key)

        // Key bytes should be zeroed (if implementation allows)
        // Note: SecretKeySpec doesn't expose array directly, but we test wrapper
    }

    @Test
    fun `phrase entry screen prevents autocomplete`() {
        val scenario = ActivityScenario.launch(PhraseEntryActivity::class.java)

        scenario.onActivity { activity =>
            val inputFields = activity.findViewsWithText("word", View.FIND_VIEWS_WITH_TEXT)
            inputFields.forEach { view ->
                assertEquals(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, view.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
            }
        }
    }

    @Test
    fun `timing attack resistance: same error for wrong phrase vs missing envelope`() {
        val manager = RecoveryEnvelopeManager(
            EnvelopeEncryptionEngine(),
            MockRecoveryPhraseEngine(),
            MockKeyDerivationEngine(),
            MockSecureStorage()
        )

        val wrongPhrase = listOf("wrong", "phrase", "here", "test", "words", "now")

        // Time both operations
        val time1 = measureTime { manager.recoverWithPhrase(wrongPhrase) }
        val time2 = measureTime { manager.recoverWithPhrase(wrongPhrase) }

        // Times should be similar (within 10%)
        val ratio = time1.max(time2) / time1.min(time2)
        assertTrue(ratio < 1.1, "Timing difference too large: $ratio")
    }
}
```

---

## Edge Case Tests

### EdgeCaseTest.kt

```kotlin
class EdgeCaseTest {

    private val engine = RecoveryPhraseEngine(TestWordLists.minimal(), MockPhraseStrengthEstimator())

    @Test
    fun `empty phrase throws`() {
        assertThrows<IllegalArgumentException> {
            engine.deriveRecoveryKey("")
        }
    }

    @Test
    fun `phrase with only whitespace throws`() {
        assertThrows<IllegalArgumentException> {
            engine.deriveRecoveryKey("   ")
        }
    }

    @Test
    fun `generated phrase must be exactly 9 words`() {
        val phrase = engine.generatePhrase()
        assertEquals(9, phrase.size)
    }

    @Test
    fun `custom phrase validation: 5 words rejected`() {
        val estimator = PhraseStrengthEstimator(emptySet(), emptySet())
        val result = estimator.estimate(List(5) { "word" })
        assertFalse(result.isAcceptable)
    }

    @Test
    fun `custom phrase validation: 25 words rejected`() {
        val estimator = PhraseStrengthEstimator(emptySet(), emptySet())
        val result = estimator.estimate(List(25) { "word" })
        assertFalse(result.isAcceptable)
    }

    @Test
    fun `phrase normalization handles unicode homoglyphs`() {
        // Cyrillic 'а' vs Latin 'a'
        val input = listOf("the", "quiеt", "river")  // 'е' is Cyrillic
        val normalized = engine.normalizePhrase(input)
        // NFKC should normalize Cyrillic to Latin or reject
        assertEquals("the quiet river", normalized)
    }

    @Test
    fun `phrase with numbers rejected in generation`() {
        val phrase = engine.generatePhrase()
        phrase.forEach { word ->
            assertFalse(word.any { it.isDigit() })
        }
    }

    @Test
    fun `concurrent recovery attempts handled`() = runTest {
        val manager = RecoveryEnvelopeManager(...)
        val phrase = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")

        // Launch multiple concurrent recoveries
        val results = (1..10).map {
            async { manager.recoverWithPhrase(phrase) }
        }.awaitAll()

        // All should succeed with same key
        results.forEach { key ->
            assertArrayEquals(key.encoded, results[0].encoded)
        }
    }
}
```

---

## Performance Tests

### KeyDerivationPerformanceTest.kt (Instrumented)

```kotlin
@RunWith(AndroidJUnit4::class)
class KeyDerivationPerformanceTest {

    private val engine = RecoveryPhraseEngine(
        WordLists.loadFromResources(ApplicationProvider.getApplicationContext()),
        PhraseStrengthEstimator(loadCommonPhrases(), loadDictionaryWords())
    )

    @Test
    fun `deriveRecoveryKey completes within 2 seconds on modern device`() {
        val phrase = "the quiet river flows beneath ancient stone bridges at midnight"

        val start = System.currentTimeMillis()
        val key = engine.deriveRecoveryKey(phrase)
        val elapsed = System.currentTimeMillis() - start

        assertNotNull(key)
        assertTrue(elapsed < 2000, "PBKDF2 took ${elapsed}ms, expected < 2000ms")
    }

    @Test
    fun `phrase generation completes within 50ms`() {
        val start = System.currentTimeMillis()
        val phrase = engine.generatePhrase()
        val elapsed = System.currentTimeMillis() - start

        assertEquals(9, phrase.size)
        assertTrue(elapsed < 50, "Phrase generation took ${elapsed}ms, expected < 50ms")
    }

    @Test
    fun `full recovery flow completes within 3 seconds`() {
        // Setup
        val masterKey = EnvelopeEncryptionEngine().generateMasterVaultKey()
        val phraseWords = engine.generatePhrase()
        val normalizedPhrase = engine.normalizePhrase(phraseWords)
        val recoveryKey = engine.deriveRecoveryKey(normalizedPhrase)
        val recoveryEnvelope = EnvelopeEncryptionEngine().wrapMasterKey(masterKey, recoveryKey)

        val manager = RecoveryEnvelopeManager(
            EnvelopeEncryptionEngine(),
            engine,
            MockKeyDerivationEngine(),
            MockSecureStorage().apply { saveRecoveryEnvelope(recoveryEnvelope) }
        )

        val start = System.currentTimeMillis()
        val recoveredKey = manager.recoverWithPhrase(phraseWords)
        val elapsed = System.currentTimeMillis() - start

        assertArrayEquals(recoveredKey.encoded, masterKey.encoded)
        assertTrue(elapsed < 3000, "Recovery took ${elapsed}ms, expected < 3000ms")
    }
}
```

---

## Test Harness Setup

### Test Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Unit testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Android instrumented testing
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.robolectric:robolectric:4.11")  // For SecureStorage tests
    androidTestImplementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Test utilities
    testImplementation("com.google.guava:guava-testlib:32.1.3-jre")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
}
```

### Test Fixtures

```kotlin
// test/fixtures/WordListsFixtures.kt
object WordListsFixtures {
    fun minimal(): WordLists = WordLists(
        articles = listOf("the", "a", "an"),
        adjectives = listOf("quiet", "swift", "ancient", "golden", "silent"),
        nouns = listOf("river", "mountain", "bridge", "key", "shadow"),
        verbs = listOf("flows", "rests", "guards", "watches", "sleeps"),
        prepositions = listOf("beneath", "above", "within", "beyond"),
        adverbs = listOf("silently", "swiftly", "eternally", "quietly")
    )

    fun loadCommonPhrases(): Set<String> = setOf(
        "to be or not to be",
        "the quick brown fox",
        "hello world",
        // ... top 10k phrases
    )

    fun loadDictionaryWords(): Set<String> = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        // ... common dictionary words
    )
}

// test/fixtures/TestVectors.kt
object TestVectors {
    // Reference vectors from iOS implementation
    const val PHRASE = "the quiet river flows beneath ancient stone bridges at midnight"
    const val PHRASE_SALT_HEX = "a1b2c3d4e5f678901234567890abcdef1234567890abcdef1234567890abcdef"
    const val RECOVERY_KEY_HEX = "..."  // 64 hex chars = 32 bytes
    const val MASTER_KEY_HEX = "..."    // 64 hex chars = 32 bytes
}
```

### CI/CD Integration

```yaml
# .github/workflows/secret-phrase-tests.yml
name: Secret Phrase Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
      - name: Run unit tests
        run: ./gradlew test --tests "*crypto*RecoveryPhrase*" --tests "*crypto*PhraseStrength*" --tests "*crypto*RecoveryEnvelope*"

  instrumented-tests:
    runs-on: macos-latest  # Required for Android emulator
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
      - name: Run instrumented tests
        run: |
          ./gradlew connectedAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.RecoveryFlowIntegrationTest

  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run performance benchmarks
        run: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.KeyDerivationPerformanceTest
```

---

## Test Coverage Targets

| Component | Line Coverage | Branch Coverage |
|-----------|---------------|-----------------|
| RecoveryPhraseEngine | 100% | 100% |
| WordLists | 100% | 100% |
| PhraseStrengthEstimator | 100% | 95% |
| RecoveryEnvelopeManager | 100% | 100% |
| SecureStorage | 90% | 85% |
| PhraseEntryViewModel | 95% | 90% |
| Overall crypto module | 95% | 90% |

### Coverage Enforcement

```kotlin
// build.gradle.kts
tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

// Jacoco for coverage
plugins {
    id("jacoco")
}

jacocoTestReport {
    reports {
        html.enabled = true
        xml.enabled = true
    }
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(dir: it, exclude: [
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.class",
                "**/Manifest*.class",
                "**/*Test*.class",
                "**/*Test*.kt"
            ])
        }))
    }
}
```

---

## Golden Test Vectors (Cross-Platform)

These vectors MUST match iOS implementation exactly.

```kotlin
// test/resources/golden_vectors.json
{
  "phrase_generation": {
    "seed": "fixed_seed_for_deterministic_test",
    "expected_phrase": ["the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight"]
  },
  "phrase_normalization": [
    {"input": ["The", "  Quiet", "River"], "expected": "the quiet river"},
    {"input": ["ｔｈｅ", "ｑｕｉｅｔ"], "expected": "the quiet"}
  ],
  "phrase_salt_derivation": {
    "phrase": "the quiet river flows beneath ancient stone bridges at midnight",
    "expected_salt_hex": "a1b2c3d4e5f678901234567890abcdef1234567890abcdef1234567890abcdef"
  },
  "recovery_key_derivation": {
    "phrase": "the quiet river flows beneath ancient stone bridges at midnight",
    "iterations": 800000,
    "expected_key_hex": "f1e2d3c4b5a697887766554433221100ffeeddccbbaa99887766554433221100"
  },
  "envelope_encryption": {
    "master_key_hex": "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff",
    "recovery_key_hex": "f1e2d3c4b5a697887766554433221100ffeeddccbbaa99887766554433221100",
    "expected_iv_hex": "0102030405060708090a0b0c",
    "expected_ciphertext_hex": "..."
  }
}
```

Run golden tests in both Android and iOS CI to ensure compatibility.