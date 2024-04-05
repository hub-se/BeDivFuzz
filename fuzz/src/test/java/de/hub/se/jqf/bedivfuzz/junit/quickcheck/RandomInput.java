package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomInput {
    int requested = 0;
    List<Integer> values = new ArrayList<>();

    public int getOrGenerateFresh(Random random) {
        requested++;
        int value = random.nextInt(256);
        values.add(value);
        return value;
    }

    public List<Integer> getValues() {
        return values;
    }

    public InputStream toInputStream(Random r) {
        RandomInput input = this;
        return new InputStream() {
            @Override
            public int read() {
                return input.getOrGenerateFresh(r);
            }
        };
    }
}
