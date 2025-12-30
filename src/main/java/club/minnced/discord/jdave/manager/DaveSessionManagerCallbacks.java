package club.minnced.discord.jdave.manager;

import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;

public interface DaveSessionManagerCallbacks {
    // Opcode MLS_KEY_PACKAGE (26)
    void sendMLSKeyPackage(@NonNull ByteBuffer mlsKeyPackage);

    // Opcode DAVE_PROTOCOL_READY_FOR_TRANSITION (23)
    void sendDaveProtocolReadyForTransition(int transitionId);

    // Opcode MLS_COMMIT_WELCOME (28)
    void sendMLSCommitWelcome(@NonNull ByteBuffer commitWelcomeMessage);

    // Opcode MLS_INVALID_COMMIT_WELCOME (31)
    void sendMLSInvalidCommitWelcome(int transitionId);
}
