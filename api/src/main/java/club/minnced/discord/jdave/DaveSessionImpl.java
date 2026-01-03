package club.minnced.discord.jdave;

import club.minnced.discord.jdave.ffi.LibDave;
import club.minnced.discord.jdave.ffi.LibDaveSessionBinding;
import club.minnced.discord.jdave.ffi.NativeUtils;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class DaveSessionImpl implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment session;

    private DaveSessionImpl(@NonNull Arena arena, @NonNull MemorySegment session) {
        this.arena = arena;
        this.session = session;
    }

    @NonNull
    public static DaveSessionImpl create(@Nullable String authSessionId) {
        Arena arena = Arena.ofConfined();
        try (Arena local = Arena.ofConfined()) {
            MemorySegment authSessionIdSegment =
                    authSessionId != null ? local.allocateFrom(authSessionId) : MemorySegment.NULL;
            MemorySegment session = LibDaveSessionBinding.createSession(MemorySegment.NULL, authSessionIdSegment);
            return new DaveSessionImpl(arena, session);
        } catch (Throwable e) {
            arena.close();
            throw e;
        }
    }

    private void destroy() {
        LibDaveSessionBinding.destroySession(this.session);
    }

    public void initialize(short version, long groupId, @NonNull String selfUserId) {
        try (Arena local = Arena.ofConfined()) {
            LibDaveSessionBinding.initializeSession(this.session, version, groupId, local.allocateFrom(selfUserId));
        }
    }

    public void reset() {
        LibDaveSessionBinding.resetSession(this.session);
    }

    public void setProtocolVersion(short version) {
        LibDaveSessionBinding.setProtocolVersion(this.session, version);
    }

    public short getProtocolVersion() {
        return LibDaveSessionBinding.getProtocolVersion(this.session);
    }

    public MemorySegment getKeyRatchet(@NonNull String selfUserId) {
        try (Arena local = Arena.ofConfined()) {
            return LibDaveSessionBinding.getKeyRatchet(this.session, local.allocateFrom(selfUserId));
        }
    }

    public void setExternalSender(@NonNull ByteBuffer externalSender) {
        LibDaveSessionBinding.setExternalSender(this.session, externalSender);
    }

    public void processProposals(
            @NonNull ByteBuffer proposals,
            @NonNull List<String> userIds,
            @NonNull Consumer<@NonNull ByteBuffer> sendMLSCommitWelcome) {
        MemorySegment welcome = LibDaveSessionBinding.processProposals(session, proposals, userIds);
        try {
            if (!NativeUtils.isNull(welcome)) {
                sendMLSCommitWelcome.accept(welcome.asByteBuffer());
            }
        } finally {
            if (!NativeUtils.isNull(welcome)) {
                LibDave.free(welcome);
            }
        }
    }

    // Returns whether we joined the group or not
    public boolean processWelcome(@NonNull ByteBuffer welcome, @NonNull List<@NonNull String> userIds) {
        MemorySegment roster = LibDaveSessionBinding.processWelcome(session, welcome, userIds);
        try {
            return !NativeUtils.isNull(roster);
        } finally {
            if (!NativeUtils.isNull(roster)) {
                LibDaveSessionBinding.destroyWelcomeResult(roster);
            }
        }
    }

    @NonNull
    public CommitResult processCommit(@NonNull ByteBuffer commit) {
        MemorySegment processedCommit = LibDaveSessionBinding.processCommit(session, commit);
        try {
            boolean isIgnored = LibDaveSessionBinding.isCommitIgnored(processedCommit);
            if (isIgnored) {
                return new CommitResult.Ignored();
            } else {
                return new CommitResult.Success(LibDaveSessionBinding.isCommitJoinedGroup(processedCommit));
            }
        } finally {
            LibDaveSessionBinding.destroyCommitResult(processedCommit);
        }
    }

    public void sendMarshalledKeyPackage(@NonNull Consumer<@NonNull ByteBuffer> sendPackage) {
        MemorySegment array = LibDaveSessionBinding.getMarshalledKeyPackage(session);
        try {
            sendPackage.accept(array.asByteBuffer());
        } finally {
            LibDave.free(array);
        }
    }

    @Override
    public void close() {
        this.destroy();
        this.arena.close();
    }

    public sealed interface CommitResult {
        record Ignored() implements CommitResult {}

        record Success(boolean joined) implements CommitResult {}
    }
}
