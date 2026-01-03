package club.minnced.discord.jdave.interop;

import club.minnced.discord.jdave.*;
import club.minnced.discord.jdave.DaveDecryptor.DaveDecryptResultType;
import club.minnced.discord.jdave.DaveEncryptor.DaveEncryptResultType;
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
        DaveCodec daveCodec = mapCodec(codec);
        if (daveCodec == DaveCodec.UNKNOWN) {
            return;
        }

        manager.assignSsrcToCodec(daveCodec, ssrc);
    }

    @Override
    public int getMaxEncryptedFrameSize(@NonNull MediaType mediaType, int frameSize) {
        DaveMediaType daveMediaType = mapMediaType(mediaType);
        if (daveMediaType == DaveMediaType.UNKNOWN) {
            return frameSize;
        }

        return manager.getMaxEncryptedFrameSize(daveMediaType, frameSize);
    }

    @Override
    public int getMaxDecryptedFrameSize(@NonNull MediaType mediaType, long userId, int frameSize) {
        DaveMediaType daveMediaType = mapMediaType(mediaType);
        if (daveMediaType == DaveMediaType.UNKNOWN) {
            return frameSize;
        }

        return manager.getMaxDecryptedFrameSize(daveMediaType, userId, frameSize);
    }

    @Override
    public boolean encrypt(
            @NonNull MediaType mediaType, int ssrc, @NonNull ByteBuffer data, @NonNull ByteBuffer encrypted) {
        DaveMediaType daveMediaType = mapMediaType(mediaType);
        if (daveMediaType == DaveMediaType.UNKNOWN) {
            return false;
        }

        DaveEncryptResultType result = manager.encrypt(daveMediaType, ssrc, data, encrypted);
        return result == DaveEncryptResultType.SUCCESS;
    }

    @Override
    public boolean decrypt(
            @NonNull MediaType mediaType, long userId, @NonNull ByteBuffer encrypted, @NonNull ByteBuffer decrypted) {
        DaveMediaType daveMediaType = mapMediaType(mediaType);
        if (daveMediaType == DaveMediaType.UNKNOWN) {
            return false;
        }

        DaveDecryptResultType result = manager.decrypt(daveMediaType, userId, encrypted, decrypted);
        return result == DaveDecryptResultType.SUCCESS;
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

    @NonNull
    private DaveMediaType mapMediaType(@NonNull MediaType mediaType) {
        if (mediaType == MediaType.AUDIO) {
            return DaveMediaType.AUDIO;
        }

        return DaveMediaType.UNKNOWN;
    }

    @NonNull
    private DaveCodec mapCodec(@NonNull Codec codec) {
        if (codec == Codec.OPUS) {
            return DaveCodec.OPUS;
        }

        return DaveCodec.UNKNOWN;
    }
}
