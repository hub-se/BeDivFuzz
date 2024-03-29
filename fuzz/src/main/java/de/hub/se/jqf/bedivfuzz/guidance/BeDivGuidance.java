package de.hub.se.jqf.bedivfuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;

public interface BeDivGuidance extends Guidance {
    /**
     * Returns a reference to a {@link SplitLinearInput} which contains
     * two streams of (structural/value) parameters.
     *
     * @return  a split stream of bytes to be used by the input generator(s)
     * @throws IllegalStateException if the last {@link #hasInput()}
     *                  returned <code>false</code>
     * @throws GuidanceException if there was an I/O or other error
     *                  in generating the input stream
     */
    SplitParameterStream getSplitInput() throws GuidanceException;

}
