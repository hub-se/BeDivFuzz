package de.hub.se.jqf.fuzz.repro;

import de.hub.se.jqf.fuzz.div.SplitInput;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;

import java.io.*;

public class SplitReproInput implements SplitInput {
    private File primaryRandomFile;
    private InputStream primaryParameterStream;

    private File secondaryRandomFile;
    private InputStream secondaryParameterStream;

    public SplitReproInput(File primaryRandom, File secondaryRandom) {
        this.primaryRandomFile = primaryRandom;
        this.secondaryRandomFile = secondaryRandom;
    }

    @Override
    public void gc() {
        try {
            if (primaryParameterStream != null) {
                primaryParameterStream.close();
            }
            if (secondaryParameterStream != null) {
                secondaryParameterStream.close();
            }
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isFavored() {
        return false;
    }

    @Override
    public InputStream createPrimaryParameterStream() {
        try {
            primaryParameterStream = new BufferedInputStream(new FileInputStream(this.primaryRandomFile));
            return primaryParameterStream;
        }
        catch(IOException e) {
            e.printStackTrace();
            throw new GuidanceException("Error while reading primary random file.", e);
        }
    }

    @Override
    public InputStream createSecondaryParameterStream() {
        try {
            secondaryParameterStream = new BufferedInputStream(new FileInputStream(this.secondaryRandomFile));
            return secondaryParameterStream;
        }
        catch(IOException e) {
            e.printStackTrace();
            throw new GuidanceException("Error while reading primary random file.", e);
        }
    }
}
