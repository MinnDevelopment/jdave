package club.minnced.discord.jdave.ffi;

import static club.minnced.discord.jdave.ffi.LibDaveLookup.*;
import static club.minnced.discord.jdave.ffi.NativeUtils.toSizeT;
import static java.lang.foreign.ValueLayout.*;

import club.minnced.discord.jdave.DaveMediaType;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import org.jspecify.annotations.NonNull;

public class LibDaveDecryptorBinding {

    static final MethodHandle daveDecryptorCreate;
    static final MethodHandle daveDecryptorDestroy;
    static final MethodHandle daveDecryptorGetMaxPlaintextByteSize;
    static final MethodHandle daveDecryptorDecrypt;
    static final MethodHandle daveDecryptorTransitionToKeyRatchet;
    static final MethodHandle daveDecryptorTransitionToPassthroughMode;

    static {
        try {
            // DAVEDecryptorHandle daveDecryptorCreate(void);
            daveDecryptorCreate = find(ADDRESS, "daveDecryptorCreate");

            // void daveDecryptorDestroy(DAVEDecryptorHandle decryptor);
            daveDecryptorDestroy = findVoid("daveDecryptorDestroy", ADDRESS);

            // size_t daveDecryptorGetMaxPlaintextByteSize(DAVEDecryptorHandle decryptor, DAVEMediaType mediaType,
            // size_t encryptedFrameSize);
            daveDecryptorGetMaxPlaintextByteSize =
                    find(C_SIZE, "daveDecryptorGetMaxPlaintextByteSize", ADDRESS, JAVA_INT, C_SIZE);

            // DAVEDecryptorResultCode daveDecryptorDecrypt(DAVEDecryptorHandle decryptor, DAVEMediaType mediaType,
            // const uint8_t* encryptedFrame, size_t encryptedFrameLength, uint8_t* frame, size_t frameCapacity, size_t*
            // bytesWritten);
            daveDecryptorDecrypt = find(
                    JAVA_INT,
                    "daveDecryptorDecrypt",
                    ADDRESS,
                    JAVA_INT,
                    ADDRESS,
                    C_SIZE,
                    ADDRESS,
                    C_SIZE,
                    ADDRESS.withTargetLayout(C_SIZE));

            // void daveDecryptorTransitionToKeyRatchet(DAVEDecryptorHandle decryptor, DAVEKeyRatchetHandle keyRatchet);
            daveDecryptorTransitionToKeyRatchet = findVoid("daveDecryptorTransitionToKeyRatchet", ADDRESS, ADDRESS);

            // void daveDecryptorTransitionToPassthroughMode(DAVEDecryptorHandle decryptor, bool passthroughMode);
            daveDecryptorTransitionToPassthroughMode =
                    findVoid("daveDecryptorTransitionToPassthroughMode", ADDRESS, JAVA_BOOLEAN);
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @NonNull
    public static MemorySegment createDecryptor() {
        try {
            return (MemorySegment) daveDecryptorCreate.invoke();
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void destroyDecryptor(@NonNull MemorySegment decryptor) {
        try {
            daveDecryptorDestroy.invoke(decryptor);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static long getMaxPlaintextByteSize(
            @NonNull MemorySegment decryptor, @NonNull DaveMediaType mediaType, long encryptedFrameSize) {
        try {
            return NativeUtils.sizeToLong(daveDecryptorGetMaxPlaintextByteSize.invoke(
                    decryptor, mediaType.ordinal(), toSizeT(encryptedFrameSize)));
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static int decrypt(
            @NonNull MemorySegment decryptor,
            @NonNull DaveMediaType mediaType,
            @NonNull MemorySegment encryptedFrame,
            @NonNull MemorySegment decryptedFrame,
            @NonNull MemorySegment bytesWritten) {
        try {
            return (int) daveDecryptorDecrypt.invoke(
                    decryptor,
                    mediaType.ordinal(),
                    encryptedFrame,
                    toSizeT(encryptedFrame.byteSize()),
                    decryptedFrame,
                    toSizeT(decryptedFrame.byteSize()),
                    bytesWritten);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void transitionToKeyRatchet(@NonNull MemorySegment decryptor, @NonNull MemorySegment keyRatchet) {
        try {
            daveDecryptorTransitionToKeyRatchet.invoke(decryptor, keyRatchet);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }

    public static void transitionToPassthroughMode(@NonNull MemorySegment decryptor, boolean passthroughMode) {
        try {
            daveDecryptorTransitionToPassthroughMode.invoke(decryptor, passthroughMode);
        } catch (Throwable e) {
            throw new LibDaveBindingException(e);
        }
    }
}
