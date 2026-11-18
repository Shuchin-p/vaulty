package com.vaultaire.android.crypto

import com.vaultaire.android.crypto.fixtures.MockKeyDerivationEngine
import com.vaultaire.android.crypto.fixtures.MockRecoveryPhraseEngine
import com.vaultaire.android.crypto.fixtures.WordListsFixtures
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import javax.crypto.AEADBadTagException

class RecoveryEnvelopeManagerTest {

    private val envelopeEngine = EnvelopeEncryptionEngine()
    private val phraseEngine = MockRecoveryPhraseEngine()
    private val keyDerivation = MockKeyDerivationEngine()
    private val secureStorage = mockk<SecureStorage>(relaxed = true)
    private lateinit var manager: RecoveryEnvelopeManager

    @Before
    fun setUp() {
        manager = RecoveryEnvelopeManager(envelopeEngine, phraseEngine, keyDerivation, secureStorage)
    }

    @Test
    fun `createEnvelopes produces two envelopes wrapping same MVK`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val patternBytes = "pattern123".toByteArray()
        val phraseWords = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")

        val envelopes = manager.createEnvelopes(masterKey, patternBytes, phraseWords)

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

        val envelopes = manager.createEnvelopes(masterKey, "pattern".toByteArray(), phraseWords)
        every { secureStorage.getRecoveryEnvelope() } returns envelopes.recoveryEnvelope

        val recoveredKey = manager.recoverWithPhrase(phraseWords)
        assertArrayEquals(recoveredKey.encoded, masterKey.encoded)
    }

    @Test
    fun `recoverWithPhrase throws for incorrect phrase`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val correctPhrase = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")
        val wrongPhrase = listOf("the", "swift", "mountain", "guards", "above", "golden", "shadows", "eternally", "now")

        val envelopes = manager.createEnvelopes(masterKey, "pattern".toByteArray(), correctPhrase)
        every { secureStorage.getRecoveryEnvelope() } returns envelopes.recoveryEnvelope

        assertThrows<InvalidPhraseException> {
            manager.recoverWithPhrase(wrongPhrase)
        }
    }

    @Test
    fun `recoverWithPhrase throws for wrong word count`() {
        val masterKey = envelopeEngine.generateMasterVaultKey()
        val phraseWords = listOf("the", "quiet", "river", "flows", "beneath", "ancient", "stone", "bridges", "midnight")
        val envelopes = manager.createEnvelopes(masterKey, "pattern".toByteArray(), phraseWords)
        every { secureStorage.getRecoveryEnvelope() } returns envelopes.recoveryEnvelope

        val shortPhrase = listOf("the", "quiet", "river")
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

        val oldEnvelopes = manager.createEnvelopes(masterKey, oldPattern, oldPhrase)
        every { secureStorage.getPatternEnvelope() } returns oldEnvelopes.patternEnvelope
        every { secureStorage.getRecoveryEnvelope() } returns oldEnvelopes.recoveryEnvelope

        val newEnvelopes = manager.rotateEnvelopes(masterKey, newPattern)

        val newVdk = keyDerivation.deriveVaultKey(newPattern)
        val mvkFromNewPattern = envelopeEngine.unwrapMasterKey(newEnvelopes.patternEnvelope, newVdk)
        assertArrayEquals(mvkFromNewPattern.encoded, masterKey.encoded)

        val newPhrase = secureStorage.getEncryptedPhrase()!!
        val newRk = phraseEngine.deriveRecoveryKey(phraseEngine.normalizePhrase(newPhrase))
        val mvkFromNewPhrase = envelopeEngine.unwrapMasterKey(newEnvelopes.recoveryEnvelope, newRk)
        assertArrayEquals(mvkFromNewPhrase.encoded, masterKey.encoded)

        val oldRk = phraseEngine.deriveRecoveryKey(phraseEngine.normalizePhrase(oldPhrase))
        assertThrows<AEADBadTagException> {
            envelopeEngine.unwrapMasterKey(newEnvelopes.recoveryEnvelope, oldRk)
        }
    }
}
