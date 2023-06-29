package de.hub.se.jqf.bedivfuzz.guidance.repro;

import de.hub.se.jqf.bedivfuzz.guidance.SplitParameterStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class SplitReproParameterStream extends SplitParameterStream {
    private final InputStream structuralParameterStream;
    private final InputStream valueParameterStream;

    public SplitReproParameterStream(File structuralRandom, File valueRandom) throws IOException {
        super(null, null);
        this.structuralParameterStream = new BufferedInputStream(Files.newInputStream(structuralRandom.toPath()));
        this.valueParameterStream = new BufferedInputStream(Files.newInputStream(valueRandom.toPath()));
    }

    @Override
    public InputStream createStructuralParameterStream() {
        return structuralParameterStream;
    }

    @Override
    public InputStream createValueParameterStream() {
        return valueParameterStream;
    }

    public void close() throws IOException {
        if (structuralParameterStream != null) {
            structuralParameterStream.close();
        }
        if (valueParameterStream != null) {
            valueParameterStream.close();
        }
    }
}
