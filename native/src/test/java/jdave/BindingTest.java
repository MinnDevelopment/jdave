package jdave;

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
        try (club.minnced.discord.jdave.DaveSessionImpl session =
                club.minnced.discord.jdave.DaveSessionImpl.create(null)) {
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

        try (club.minnced.discord.jdave.DaveSessionImpl session =
                club.minnced.discord.jdave.DaveSessionImpl.create(null)) {
            session.initialize((short) 1, channelId, selfUserIdString);
            assertEquals(1, session.getProtocolVersion());
            session.sendMarshalledKeyPackage(session::setExternalSender);

            try (club.minnced.discord.jdave.DaveEncryptor encryptor =
                    club.minnced.discord.jdave.DaveEncryptor.create(session)) {
                encryptor.prepareTransition(selfUserId, 1);
                encryptor.processTransition(1);

                int ssrc = random.nextInt();
                encryptor.assignSsrcToCodec(club.minnced.discord.jdave.DaveCodec.OPUS, ssrc);

                byte[] plaintext = new byte[512];
                random.nextBytes(plaintext);

                ByteBuffer output = ByteBuffer.allocateDirect(587);
                ByteBuffer input = ByteBuffer.allocateDirect(plaintext.length);
                input.put(plaintext);
                input.flip();

                assertEquals(
                        output.capacity(),
                        encryptor.getMaxCiphertextByteSize(
                                club.minnced.discord.jdave.DaveMediaType.AUDIO, input.capacity()));

                club.minnced.discord.jdave.DaveEncryptor.DaveEncryptorResult result =
                        encryptor.encrypt(club.minnced.discord.jdave.DaveMediaType.AUDIO, ssrc, input, output);

                assertEquals(club.minnced.discord.jdave.DaveEncryptor.DaveEncryptResultType.FAILURE, result.type());
            }
        }
    }

    @Test
    void testEncryptorPassthrough() {
        Random random = new Random(42);
        long selfUserId = random.nextLong();

        try (club.minnced.discord.jdave.DaveSessionImpl session =
                club.minnced.discord.jdave.DaveSessionImpl.create(null)) {
            try (club.minnced.discord.jdave.DaveEncryptor encryptor =
                    club.minnced.discord.jdave.DaveEncryptor.create(session)) {
                encryptor.prepareTransition(
                        selfUserId, club.minnced.discord.jdave.DaveConstants.DISABLED_PROTOCOL_VERSION);
                encryptor.processTransition(club.minnced.discord.jdave.DaveConstants.DISABLED_PROTOCOL_VERSION);

                int ssrc = random.nextInt();
                encryptor.assignSsrcToCodec(club.minnced.discord.jdave.DaveCodec.OPUS, ssrc);

                byte[] plaintext = new byte[512];
                random.nextBytes(plaintext);

                ByteBuffer output = ByteBuffer.allocateDirect(512);
                ByteBuffer input = ByteBuffer.allocateDirect(plaintext.length);
                input.put(plaintext);
                input.flip();

                club.minnced.discord.jdave.DaveEncryptor.DaveEncryptorResult result =
                        encryptor.encrypt(club.minnced.discord.jdave.DaveMediaType.AUDIO, ssrc, input, output);

                assertEquals(club.minnced.discord.jdave.DaveEncryptor.DaveEncryptResultType.SUCCESS, result.type());
                assertTrue(result.bytesWritten() > 0);
                assertEquals(output.capacity(), result.bytesWritten());
            }
        }
    }
}
