package com.mikusher.formats;

/**
 * Resource limits for PMAP/XML parsing.
 */
public final class PMapParserLimits {
    private static final int DEFAULT_MAX_DEPTH = 64;
    private static final int DEFAULT_MAX_ENTRIES = 10_000;
    private static final int DEFAULT_MAX_COLLECTION_SIZE = 10_000;
    private static final int DEFAULT_MAX_TEXT_LENGTH = 1_048_576;
    private static final long DEFAULT_MAX_INPUT_BYTES = 10L * 1024L * 1024L;
    private static final long DEFAULT_MAX_INPUT_CHARACTERS = 10L * 1024L * 1024L;

    private final int maxDepth;
    private final int maxEntries;
    private final int maxCollectionSize;
    private final int maxTextLength;
    private final long maxInputBytes;
    private final long maxInputCharacters;

    private PMapParserLimits(Builder builder) {
        maxDepth = positive(builder.maxDepth, "maxDepth");
        maxEntries = positive(builder.maxEntries, "maxEntries");
        maxCollectionSize = positive(builder.maxCollectionSize, "maxCollectionSize");
        maxTextLength = positive(builder.maxTextLength, "maxTextLength");
        maxInputBytes = positive(builder.maxInputBytes, "maxInputBytes");
        maxInputCharacters = positive(builder.maxInputCharacters, "maxInputCharacters");
    }

    public static PMapParserLimits defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public int getMaxCollectionSize() {
        return maxCollectionSize;
    }

    public int getMaxTextLength() {
        return maxTextLength;
    }

    public long getMaxInputBytes() {
        return maxInputBytes;
    }

    public long getMaxInputCharacters() {
        return maxInputCharacters;
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static long positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static final class Builder {
        private int maxDepth = DEFAULT_MAX_DEPTH;
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private int maxCollectionSize = DEFAULT_MAX_COLLECTION_SIZE;
        private int maxTextLength = DEFAULT_MAX_TEXT_LENGTH;
        private long maxInputBytes = DEFAULT_MAX_INPUT_BYTES;
        private long maxInputCharacters = DEFAULT_MAX_INPUT_CHARACTERS;

        public Builder maxDepth(int value) {
            maxDepth = value;
            return this;
        }

        public Builder maxEntries(int value) {
            maxEntries = value;
            return this;
        }

        public Builder maxCollectionSize(int value) {
            maxCollectionSize = value;
            return this;
        }

        public Builder maxTextLength(int value) {
            maxTextLength = value;
            return this;
        }

        public Builder maxInputBytes(long value) {
            maxInputBytes = value;
            return this;
        }

        public Builder maxInputCharacters(long value) {
            maxInputCharacters = value;
            return this;
        }

        public PMapParserLimits build() {
            return new PMapParserLimits(this);
        }
    }
}
