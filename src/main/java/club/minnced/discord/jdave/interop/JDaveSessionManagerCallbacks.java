package club.minnced.discord.jdave.interop;

import club.minnced.discord.jdave.manager.DaveSessionManagerCallbacks;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.audio.dave.DaveProtocolCallbacks;

public class JDaveSessionManagerCallbacks implements DaveSessionManagerCallbacks {
    private final DaveProtocolCallbacks delegate;

    public JDaveSessionManagerCallbacks(DaveProtocolCallbacks delegate) {
        this.delegate = delegate;
    }

    @Override
    public void sendMLSKeyPackage(ByteBuffer mlsKeyPackage) {
        this.delegate.sendMLSKeyPackage(mlsKeyPackage);
    }

    @Override
    public void sendDaveProtocolReadyForTransition(int transitionId) {
        this.delegate.sendDaveProtocolReadyForTransition(transitionId);
    }

    @Override
    public void sendMLSCommitWelcome(ByteBuffer commitWelcomeMessage) {
        this.delegate.sendMLSCommitWelcome(commitWelcomeMessage);
    }

    @Override
    public void sendMLSInvalidCommitWelcome(int transitionId) {
        this.delegate.sendMLSInvalidCommitWelcome(transitionId);
    }
}
