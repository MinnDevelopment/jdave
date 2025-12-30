package club.minnced.discord.jdave.manager;

import java.nio.ByteBuffer;

public interface DaveSessionManagerCallbacks {
    // Opcode MLS_KEY_PACKAGE (26)
    void sendMLSKeyPackage(ByteBuffer mlsKeyPackage);

    // Opcode DAVE_PROTOCOL_READY_FOR_TRANSITION (23)
    void sendDaveProtocolReadyForTransition(int transitionId);

    // Opcode MLS_COMMIT_WELCOME (28)
    void sendMLSCommitWelcome(ByteBuffer commitWelcomeMessage);

    // Opcode MLS_INVALID_COMMIT_WELCOME (31)
    void sendMLSInvalidCommitWelcome(int transitionId);
}
