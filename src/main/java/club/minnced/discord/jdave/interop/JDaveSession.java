package club.minnced.discord.jdave.interop;

import club.minnced.discord.jdave.*;
import club.minnced.discord.jdave.manager.DaveSessionManager;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.audio.dave.DaveProtocolCallbacks;
import net.dv8tion.jda.api.audio.dave.DaveSession;
import org.jspecify.annotations.NonNull;

public class JDaveSession implements DaveSession {
    private final DaveSessionManager manager;

    public JDaveSession(long selfUserId, long channelId, @NonNull DaveProtocolCallbacks callbacks) {
        this.manager = DaveSessionManager.create(selfUserId, channelId, new JDaveSessionManagerCallbacks(callbacks));
    }

    @Override
    public int getMaxProtocolVersion() {
        return manager.getMaxProtocolVersion();
    }

    @Override
    public void assignSsrcToCodec(@NonNull Codec codec, int ssrc) {
        if (codec == Codec.OPUS) {
            manager.assignSsrcToCodec(DaveCodec.OPUS, ssrc);
        }
    }

    @Override
    public int getMaxEncryptedFrameSize(@NonNull MediaType type, int frameSize) {
        if (type != MediaType.AUDIO) {
            return frameSize * 2;
        }

        return manager.getMaxEncryptedFrameSize(DaveMediaType.AUDIO, frameSize);
    }

    @Override
    public int getMaxDecryptedFrameSize(@NonNull MediaType type, long userId, int frameSize) {
        if (type != MediaType.AUDIO) {
            return frameSize * 2;
        }

        return manager.getMaxDecryptedFrameSize(DaveMediaType.AUDIO, userId, frameSize);
    }

    @Override
    public void encryptOpus(int ssrc, @NonNull ByteBuffer audio, @NonNull ByteBuffer encrypted) {
        manager.encrypt(DaveMediaType.AUDIO, ssrc, audio, encrypted);
    }

    @Override
    public void decryptOpus(long userId, @NonNull ByteBuffer encrypted, @NonNull ByteBuffer decrypted) {
        manager.decrypt(DaveMediaType.AUDIO, userId, encrypted, decrypted);
    }

    @Override
    public void addUser(long userId) {
        manager.addUser(userId);
    }

    @Override
    public void removeUser(long userId) {
        manager.removeUser(userId);
    }

    @Override
    public void initialize() {}

    @Override
    public void destroy() {
        manager.close();
    }

    @Override
    public void onSelectProtocolAck(int protocolVersion) {
        manager.onSelectProtocolAck(protocolVersion);
    }

    @Override
    public void onDaveProtocolPrepareTransition(int transitionId, int protocolVersion) {
        manager.onDaveProtocolPrepareTransition(transitionId, protocolVersion);
    }

    @Override
    public void onDaveProtocolExecuteTransition(int transitionId) {
        manager.onDaveProtocolExecuteTransition(transitionId);
    }

    @Override
    public void onDaveProtocolPrepareEpoch(@NonNull String epoch, int protocolVersion) {
        manager.onDaveProtocolPrepareEpoch(epoch, protocolVersion);
    }

    @Override
    public void onDaveProtocolMLSExternalSenderPackage(@NonNull ByteBuffer externalSenderPackage) {
        manager.onDaveProtocolMLSExternalSenderPackage(externalSenderPackage);
    }

    @Override
    public void onMLSProposals(@NonNull ByteBuffer proposals) {
        manager.onMLSProposals(proposals);
    }

    @Override
    public void onMLSPrepareCommitTransition(int transitionId, @NonNull ByteBuffer commit) {
        manager.onMLSPrepareCommitTransition(transitionId, commit);
    }

    @Override
    public void onMLSWelcome(int transitionId, @NonNull ByteBuffer welcome) {
        manager.onMLSWelcome(transitionId, welcome);
    }
}
