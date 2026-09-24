package com.mikusher.formats;

import com.mikusher.parameter.PMapType;
import com.mikusher.parameter.ParameterMap;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StreamedPMapParserConcurrencyTest {

    @Test
    public void manuallyConstructedParserCanBeSharedAcrossThreads() throws Exception {
        final StreamedPMapParser parser = new StreamedPMapParser(PMapParserLimits.defaults());
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 64; i++) {
                tasks.add(() -> {
                    for (int iteration = 0; iteration < 50; iteration++) {
                        Date parsed = (Date) parser.parseValueLeaf("20260924213000", PMapType.DATE);
                        assertNotNull(parsed);

                        String xml = "<m><s n=\"value\">satellite</s></m>";
                        ParameterMap map = parser.getMap(new ByteArrayInputStream(
                                xml.getBytes(StandardCharsets.UTF_8)));
                        assertEquals("satellite", map.get("value"));
                    }
                    return null;
                });
            }

            for (Future<Void> future : executor.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
