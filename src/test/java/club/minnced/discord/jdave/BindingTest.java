package club.minnced.discord.jdave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import club.minnced.discord.jdave.ffi.LibDave;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BindingTest {
    private static final Logger log = LoggerFactory.getLogger(BindingTest.class);

    @BeforeEach
    void before(TestInfo testInfo) {
        log.info("Starting test: {}", testInfo.getDisplayName());
    }

    @Test
    void testMaxVersion() {
        assertEquals(1, LibDave.getMaxSupportedProtocolVersion());
    }

    @Test
    void testSessionCreateDestroy() {
        Random random = new Random(42);
        try (DaveSessionImpl session = DaveSessionImpl.create(null)) {
            session.initialize((short) 1, random.nextLong(), Long.toUnsignedString(random.nextLong()));
            assertEquals(1, session.getProtocolVersion());
        }
    }

    @Test
    void testEncryptor() {
        Random random = new Random(42);
        long channelId = random.nextLong();
        long selfUserId = random.nextLong();
        String selfUserIdString = Long.toUnsignedString(selfUserId);

        try (DaveSessionImpl session = DaveSessionImpl.create(null)) {
            session.initialize((short) 1, channelId, selfUserIdString);
            assertEquals(1, session.getProtocolVersion());
            session.sendMarshalledKeyPackage(session::setExternalSender);

            try (DaveEncryptor encryptor = DaveEncryptor.create(session)) {
                encryptor.prepareTransition(selfUserId, 1);
                encryptor.processTransition(1);

                int ssrc = random.nextInt();
                encryptor.assignSsrcToCodec(DaveCodec.OPUS, ssrc);

                byte[] plaintext = new byte[512];
                random.nextBytes(plaintext);

                ByteBuffer output = ByteBuffer.allocateDirect(587);
                ByteBuffer input = ByteBuffer.allocateDirect(plaintext.length);
                input.put(plaintext);
                input.flip();

                assertEquals(
                        output.capacity(), encryptor.getMaxCiphertextByteSize(DaveMediaType.AUDIO, input.capacity()));

                DaveEncryptor.DaveEncryptorResult result = encryptor.encrypt(DaveMediaType.AUDIO, ssrc, input, output);

                assertEquals(DaveEncryptor.DaveEncryptResultType.FAILURE, result.type());
            }
        }
    }

    @Test
    void testEncryptorPassthrough() {
        Random random = new Random(42);
        long selfUserId = random.nextLong();

        try (DaveSessionImpl session = DaveSessionImpl.create(null)) {
            try (DaveEncryptor encryptor = DaveEncryptor.create(session)) {
                encryptor.prepareTransition(selfUserId, DaveConstants.DISABLED_PROTOCOL_VERSION);
                encryptor.processTransition(DaveConstants.DISABLED_PROTOCOL_VERSION);

                int ssrc = random.nextInt();
                encryptor.assignSsrcToCodec(DaveCodec.OPUS, ssrc);

                byte[] plaintext = new byte[512];
                random.nextBytes(plaintext);

                ByteBuffer output = ByteBuffer.allocateDirect(512);
                ByteBuffer input = ByteBuffer.allocateDirect(plaintext.length);
                input.put(plaintext);
                input.flip();

                DaveEncryptor.DaveEncryptorResult result = encryptor.encrypt(DaveMediaType.AUDIO, ssrc, input, output);

                assertEquals(DaveEncryptor.DaveEncryptResultType.SUCCESS, result.type());
                assertTrue(result.bytesWritten() > 0);
                assertEquals(output.capacity(), result.bytesWritten());
            }
        }
    }
}
