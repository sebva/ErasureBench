package ch.unine.vauchers.erasuretester.erasure;

import ch.unine.vauchers.erasuretester.backend.StorageBackend;
import ch.unine.vauchers.erasuretester.erasure.codes.SimpleRegeneratingCode;
import ch.unine.vauchers.erasuretester.erasure.codes.TooManyErasedLocations;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Intermediate layer between the frontend, the storage backend and erasure coding.
 * Special subclass to account for the differences of {@link SimpleRegeneratingCode}.
 */
public class SimpleRegeneratingFileEncoderDecoder extends FileEncoderDecoder {
    /**
     * Constructor
     *
     * @param erasureCode    The erasure coding implementation to use
     * @param storageBackend The storage backend implementation to use
     */
    public SimpleRegeneratingFileEncoderDecoder(@NotNull SimpleRegeneratingCode erasureCode, @NotNull StorageBackend storageBackend) {
        super(erasureCode, storageBackend);
    }

    @Override
    protected Stream<Byte> decodeFileData(IntList blockKeys, IntList erasedIndices) throws TooManyErasedLocations {
        Arrays.fill(dataBuffer, 0, totalSize, 0);
        Arrays.fill(stripeBuffer, 0, stripeSize, 0);

        final IntList locationsToReadForDecode;

        //erasureCode.locationsToReadForDecode(erasedIndices);
        locationsToReadForDecode = erasureCode.locationsToReadForDecode(erasedIndices);
        locationsToReadForDecode.sort(null);

        for (int locationToRead : locationsToReadForDecode) {
            final Integer value = storageBackend.retrieveBlock(blockKeys.getInt(locationToRead)).orElseThrow(RuntimeException::new);
            dataBuffer[locationToRead] = value;
            if (locationToRead >= paritySize) {
                stripeBuffer[locationToRead - paritySize] = value;
            }
        }

        int[] recoveredValues = new int[erasedIndices.size()];
        final int[] locationsNotToRead = fillNotToRead(locationsToReadForDecode);
        erasureCode.decode(dataBuffer, erasedIndices.toIntArray(), recoveredValues, locationsToReadForDecode.toIntArray(), locationsNotToRead);
        restoreValues(stripeBuffer, erasedIndices.toIntArray(), recoveredValues);

        for (int i = 0; i < stripeSize; i++) {
            if (stripeBuffer[i] == 0) { // Not present, or 1/256 chance that the value is 0
                stripeBuffer[i] = storageBackend.retrieveBlock(blockKeys.getInt(i + paritySize)).orElse(0);
            }
        }
        return Arrays.stream(stripeBuffer).boxed().map(Integer::byteValue);
    }

    private void restoreValues(int[] data, int[] recoveredIndices, int[] recoveredValues) {
        for (int i = 0; i < recoveredIndices.length; i++) {
            if (recoveredIndices[i] >= paritySize) {
                data[recoveredIndices[i] - paritySize] = recoveredValues[i];
            }
        }
    }

    @Override
    public String toString() {
        return "SimpleRegeneratingFileEncoderDecoder{" + super.toString() + "}";
    }
}
