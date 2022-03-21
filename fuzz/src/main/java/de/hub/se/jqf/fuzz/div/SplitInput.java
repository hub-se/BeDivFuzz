package de.hub.se.jqf.fuzz.div;

import java.io.InputStream;

public interface SplitInput {

    void gc();

    int size();

    boolean isFavored();

    InputStream createPrimaryParameterStream();

    InputStream createSecondaryParameterStream();
}
