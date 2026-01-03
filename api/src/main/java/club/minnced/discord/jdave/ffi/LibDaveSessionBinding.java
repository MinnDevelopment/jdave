package club.minnced.discord.jdave.ffi;

import static club.minnced.discord.jdave.ffi.LibDave.*;
import static club.minnced.discord.jdave.ffi.NativeUtils.toSizeT;
import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class LibDaveSessionBinding {
    static final MethodHandle daveSessionCreate;
    static final MethodHandle daveSessionDestroy;
    static final MethodHandle daveSessionInit;
    static final MethodHandle daveSessionReset;
    static final MethodHandle daveSessionSetProtocolVersion;
    static final MethodHandle daveSessionGetProtocolVersion;
    static final MethodHandle daveSessionGetMarshalledKeyPackage;
    static final MethodHandle daveSessionGetKeyRatchet;
    static final MethodHandle daveSessionGetLastEpochAuthenticator;
    static final MethodHandle daveSessionSetExternalSender;
    static final MethodHandle daveSessionProcessProposals;
    static final MethodHandle daveSessionProcessCommit;
    static final MethodHandle daveCommitResultIsIgnored;
    static final MethodHandle daveCommitResultIsFailed;
    static final MethodHandle daveCommitResultDestroy;
    static final MethodHandle daveSessionProcessWelcome;
    static final MethodHandle daveWelcomeResultDestroy;

    static {
        try {
            // DAVESessionHandle daveSessionCreate(
            //   void* context, const char* authSessionId, DAVEMLSFailureCallback callback);
            daveSessionCreate = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionCreate").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

            // void daveSessionDestroy(DAVESessionHandle session);
            daveSessionDestroy = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionDestroy").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

            // void daveSessionInit(
            //   DAVESessionHandle session, uint16_t version, uint64_t groupId, const char* selfUserId);
            daveSessionInit = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionInit").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_SHORT, JAVA_LONG, ADDRESS));

            // void daveSessionReset(DAVESessionHandle session);
            daveSessionReset = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionReset").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

            // void daveSessionSetProtocolVersion(DAVESessionHandle session, uint16_t version);
            daveSessionSetProtocolVersion = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionSetProtocolVersion").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_SHORT));

            // uint16_t daveSessionGetProtocolVersion(DAVESessionHandle session);
            daveSessionGetProtocolVersion = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionGetProtocolVersion").orElseThrow(),
                    FunctionDescriptor.of(JAVA_SHORT, ADDRESS));

            // void daveSessionGetMarshalledKeyPackage(
            //   DAVESessionHandle session, uint8_t** keyPackage, size_t* length);
            daveSessionGetMarshalledKeyPackage = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionGetMarshalledKeyPackage").orElseThrow(),
                    FunctionDescriptor.ofVoid(
                            ADDRESS, ADDRESS.withTargetLayout(ADDRESS), ADDRESS.withTargetLayout(C_SIZE)));

            // DAVEKeyRatchetHandle daveSessionGetKeyRatchet(DAVESessionHandle session, const char* userId);
            daveSessionGetKeyRatchet = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionGetKeyRatchet").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

            // void daveSessionGetLastEpochAuthenticator(
            //   DAVESessionHandle session, uint8_t** authenticator, size_t* length);
            daveSessionGetLastEpochAuthenticator = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionGetLastEpochAuthenticator").orElseThrow(),
                    FunctionDescriptor.ofVoid(
                            ADDRESS, ADDRESS.withTargetLayout(ADDRESS), ADDRESS.withTargetLayout(C_SIZE)));

            // void daveSessionSetExternalSender(
            //   DAVESessionHandle session, uint8_t* externalSender, size_t length);
            daveSessionSetExternalSender = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionSetExternalSender").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, C_SIZE));

            // void daveSessionProcessProposals(
            //   DAVESessionHandle session, uint8_t* proposals, size_t length, char** recognizedUserIds,
            //   size_t recognizedUserIdsLength, uint8_t** commitWelcomeBytes, size_t* commitWelcomeBytesLength);
            daveSessionProcessProposals = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionProcessProposals").orElseThrow(),
                    FunctionDescriptor.ofVoid(
                            ADDRESS,
                            ADDRESS.withTargetLayout(JAVA_BYTE),
                            C_SIZE,
                            ADDRESS,
                            C_SIZE,
                            ADDRESS,
                            ADDRESS.withTargetLayout(C_SIZE)));

            // DAVECommitResultHandle daveSessionProcessCommit(
            //   DAVESessionHandle session, uint8_t* commit, size_t length);
            daveSessionProcessCommit = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionProcessCommit").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, C_SIZE));

            // bool daveCommitResultIsIgnored(DAVECommitResultHandle commitResultHandle);
            daveCommitResultIsIgnored = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveCommitResultIsIgnored").orElseThrow(),
                    FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS));

            // bool daveCommitResultIsFailed(DAVECommitResultHandle commitResultHandle);
            daveCommitResultIsFailed = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveCommitResultIsFailed").orElseThrow(),
                    FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS));

            // void daveCommitResultDestroy(DAVECommitResultHandle commitResultHandle);
            daveCommitResultDestroy = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveCommitResultDestroy").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

            // DAVEWelcomeResultHandle daveSessionProcessWelcome(
            //   DAVESessionHandle session, uint8_t* welcome, size_t length,
            //   char** recognizedUserIds, size_t recognizedUserIdsLength);
            daveSessionProcessWelcome = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveSessionProcessWelcome").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, C_SIZE, ADDRESS, C_SIZE));

            // void daveWelcomeResultDestroy(DAVEWelcomeResultHandle welcomeResultHandle);
            daveWelcomeResultDestroy = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveWelcomeResultDestroy").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @NonNull
    public static MemorySegment createSession(@NonNull MemorySegment context, @NonNull MemorySegment authSessionId) {
        try {
            return (MemorySegment) daveSessionCreate.invoke(context, authSessionId, MemorySegment.NULL);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void destroySession(@NonNull MemorySegment session) {
        try {
            daveSessionDestroy.invoke(session);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void initializeSession(
            @NonNull MemorySegment session, short version, long groupId, @NonNull MemorySegment selfUserId) {
        try {
            daveSessionInit.invoke(session, version, groupId, selfUserId);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void resetSession(@NonNull MemorySegment session) {
        try {
            daveSessionReset.invoke(session);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void setProtocolVersion(@NonNull MemorySegment session, short version) {
        try {
            daveSessionSetProtocolVersion.invoke(session, version);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static short getProtocolVersion(@NonNull MemorySegment session) {
        try {
            return (short) daveSessionGetProtocolVersion.invoke(session);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    @NonNull
    public static MemorySegment getMarshalledKeyPackage(@NonNull MemorySegment session) {
        try (Arena local = Arena.ofConfined()) {
            MemorySegment sizePtr = local.allocate(C_SIZE);
            MemorySegment arrayPtr = local.allocate(ADDRESS.withTargetLayout(ADDRESS));

            daveSessionGetMarshalledKeyPackage.invoke(session, arrayPtr, sizePtr);

            return getByteArrayFromRawParts(arrayPtr, sizePtr);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    @NonNull
    public static MemorySegment getKeyRatchet(@NonNull MemorySegment session, @NonNull MemorySegment selfUserId) {
        try {
            return (MemorySegment) daveSessionGetKeyRatchet.invoke(session, selfUserId);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    @NonNull
    public static MemorySegment getLastEpochAuthenticator(@NonNull MemorySegment session) {
        try (Arena local = Arena.ofConfined()) {
            MemorySegment sizePtr = local.allocate(C_SIZE);
            MemorySegment arrayPtr = local.allocate(ADDRESS.withTargetLayout(ADDRESS));
            daveSessionGetLastEpochAuthenticator.invoke(session, arrayPtr, sizePtr);

            return getByteArrayFromRawParts(arrayPtr, sizePtr);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void setExternalSender(@NonNull MemorySegment session, @NonNull ByteBuffer externalSenderPackage) {
        try {
            daveSessionSetExternalSender.invoke(
                    session, MemorySegment.ofBuffer(externalSenderPackage), externalSenderPackage.remaining());
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    // Returns Welcome package
    @NonNull
    public static MemorySegment processProposals(
            @NonNull MemorySegment session,
            @NonNull ByteBuffer proposals,
            @NonNull List<@NonNull String> recognizedUserIds) {
        try (Arena local = Arena.ofConfined()) {
            MemorySegment welcomeSizePtr = local.allocate(C_SIZE);
            MemorySegment welcomeArrayPtr = local.allocate(ADDRESS.withTargetLayout(ADDRESS));
            MemorySegment recognizedUserIdsArray = allocateStringArray(local, recognizedUserIds);

            daveSessionProcessProposals.invoke(
                    session,
                    MemorySegment.ofBuffer(proposals),
                    toSizeT(proposals.remaining()),
                    recognizedUserIdsArray,
                    toSizeT(recognizedUserIds.size()),
                    welcomeArrayPtr,
                    welcomeSizePtr);

            return getByteArrayFromRawParts(welcomeArrayPtr, welcomeSizePtr);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    @NonNull
    public static MemorySegment processCommit(@NonNull MemorySegment session, @NonNull ByteBuffer commit) {
        try {
            return (MemorySegment) daveSessionProcessCommit.invoke(
                    session, MemorySegment.ofBuffer(commit), toSizeT(commit.remaining()));
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static boolean isCommitIgnored(@NonNull MemorySegment processedCommit) {
        try {
            return (boolean) daveCommitResultIsIgnored.invoke(processedCommit);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static boolean isCommitFailure(@NonNull MemorySegment processedCommit) {
        try {
            return (boolean) daveCommitResultIsFailed.invoke(processedCommit);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static boolean isCommitJoinedGroup(@NonNull MemorySegment processedCommit) {
        return !isCommitIgnored(processedCommit) && !isCommitFailure(processedCommit);
    }

    public static void destroyCommitResult(@NonNull MemorySegment processedCommit) {
        try {
            daveCommitResultDestroy.invoke(processedCommit);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    // Returns a "roster" of users / keys or null
    @NonNull
    public static MemorySegment processWelcome(
            @NonNull MemorySegment session,
            @NonNull ByteBuffer welcome,
            @NonNull List<@NonNull String> recognizedUserIds) {
        try (Arena local = Arena.ofConfined()) {
            MemorySegment recognizedUserIdsArray = allocateStringArray(local, recognizedUserIds);

            return (MemorySegment) daveSessionProcessWelcome.invoke(
                    session,
                    MemorySegment.ofBuffer(welcome),
                    toSizeT(welcome.remaining()),
                    recognizedUserIdsArray,
                    toSizeT(recognizedUserIds.size()));
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void destroyWelcomeResult(@NonNull MemorySegment welcomeResult) {
        try {
            daveWelcomeResultDestroy.invoke(welcomeResult);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    @NonNull
    private static MemorySegment getByteArrayFromRawParts(
            @NonNull MemorySegment arrayPtr, @NonNull MemorySegment sizePtr) {
        long size = readSize(sizePtr);
        AddressLayout arrayLayout = ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(size, JAVA_BYTE));
        return arrayPtr.get(arrayLayout, 0).asSlice(0, size);
    }

    @NonNull
    private static MemorySegment allocateStringArray(@NonNull Arena arena, @NonNull List<@NonNull String> userIds) {
        MemorySegment recognizedUserIdsArray = arena.allocate(MemoryLayout.sequenceLayout(userIds.size(), ADDRESS));

        for (int i = 0; i < userIds.size(); i++) {
            recognizedUserIdsArray.setAtIndex(ADDRESS, i, arena.allocateFrom(userIds.get(i)));
        }

        return recognizedUserIdsArray;
    }
}
