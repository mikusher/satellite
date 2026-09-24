package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteEntry;
import io.github.mikusher.satellite.egress.SatelliteMap;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class HmacSha256TokenizerTest {

    @Test
    public void tokenIsDeterministicAndDomainSeparatedByKey() {
        byte[] secret = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        HmacSha256Tokenizer tokenizer = new HmacSha256Tokenizer(secret);

        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);
        Key<String> username = Key.string("username")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        SatelliteEntry<String> emailEntry = SatelliteMap.builder()
                .put(email, "same-value").build().entry(email).get();
        SatelliteEntry<String> usernameEntry = SatelliteMap.builder()
                .put(username, "same-value").build().entry(username).get();

        String first = tokenizer.tokenize(emailEntry);
        String second = tokenizer.tokenize(emailEntry);
        String otherKey = tokenizer.tokenize(usernameEntry);

        assertEquals(first, second);
        assertNotEquals(first, otherKey);
        assertFalse(first.contains("same-value"));
    }
}
