package club.minnced.discord.jdave.ffi;

import static club.minnced.discord.jdave.ffi.LibDave.*;
import static club.minnced.discord.jdave.ffi.NativeUtils.toSizeT;
import static java.lang.foreign.ValueLayout.*;

import club.minnced.discord.jdave.DaveMediaType;
import java.lang.foreign.FunctionDescriptor;
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
            daveDecryptorCreate = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveDecryptorCreate").orElseThrow(), FunctionDescriptor.of(ADDRESS));

            // void daveDecryptorDestroy(DAVEDecryptorHandle decryptor);
            daveDecryptorDestroy = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveDecryptorDestroy").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

            // size_t daveDecryptorGetMaxPlaintextByteSize(DAVEDecryptorHandle decryptor, DAVEMediaType mediaType,
            // size_t encryptedFrameSize);
            daveDecryptorGetMaxPlaintextByteSize = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveDecryptorGetMaxPlaintextByteSize").orElseThrow(),
                    FunctionDescriptor.of(C_SIZE, ADDRESS, JAVA_INT, C_SIZE));

            // DAVEDecryptorResultCode daveDecryptorDecrypt(DAVEDecryptorHandle decryptor, DAVEMediaType mediaType,
            // const uint8_t* encryptedFrame, size_t encryptedFrameLength, uint8_t* frame, size_t frameCapacity, size_t*
            // bytesWritten);
            daveDecryptorDecrypt = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveDecryptorDecrypt").orElseThrow(),
                    FunctionDescriptor.of(
                            JAVA_INT,
                            ADDRESS,
                            JAVA_INT,
                            ADDRESS,
                            C_SIZE,
                            ADDRESS,
                            C_SIZE,
                            ADDRESS.withTargetLayout(C_SIZE)));

            // void daveDecryptorTransitionToKeyRatchet(DAVEDecryptorHandle decryptor, DAVEKeyRatchetHandle keyRatchet);
            daveDecryptorTransitionToKeyRatchet = LINKER.downcallHandle(
                    SYMBOL_LOOKUP.find("daveDecryptorTransitionToKeyRatchet").orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

            // void daveDecryptorTransitionToPassthroughMode(DAVEDecryptorHandle decryptor, bool passthroughMode);
            daveDecryptorTransitionToPassthroughMode = LINKER.downcallHandle(
                    SYMBOL_LOOKUP
                            .find("daveDecryptorTransitionToPassthroughMode")
                            .orElseThrow(),
                    FunctionDescriptor.ofVoid(ADDRESS, JAVA_BOOLEAN));
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
            return sizeToLong(daveDecryptorGetMaxPlaintextByteSize.invoke(
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
