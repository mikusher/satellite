package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.SatelliteEntry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class HmacSha256Tokenizer implements Tokenizer {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public HmacSha256Tokenizer(byte[] secret) {
        Objects.requireNonNull(secret, "secret");
        if (secret.length < 32) {
            throw new IllegalArgumentException("HMAC secret must be at least 32 bytes");
        }
        this.secret = secret.clone();
    }

    @Override
    public String tokenize(SatelliteEntry<?> entry) {
        Objects.requireNonNull(entry, "entry");
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            mac.update(entry.getKey().getName().getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            mac.update(stableBytes(entry.getValue()));
            return "hmac-sha256:"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to tokenize value", e);
        }
    }

    private static byte[] stableBytes(Object value) {
        if (value == null) {
            return new byte[]{0};
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        if (value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum) {
            return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException(
                "No deterministic token representation for " + value.getClass().getName());
    }
}
