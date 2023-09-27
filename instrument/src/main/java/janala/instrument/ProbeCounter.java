package janala.instrument;

public class ProbeCounter {
    public static ProbeCounter instance = new ProbeCounter();

    // TODO: Do we need to use AtomicIntegers instead?
    private int numTotalProbes = 0;
    private int numSemanticProbes = 0;

    public void incrementTotalProbes() {
        numTotalProbes++;
    }

    public void addTotalProbes(int delta) {
        numTotalProbes += delta;
    }

    public void incrementSemanticProbes() {
        numSemanticProbes++;
    }

    public void addSemanticProbes(int delta) {
        numSemanticProbes += delta;
    }

    public int getNumTotalProbes() {
        return numTotalProbes;
    }

    public int getNumSemanticProbes() {
        return numSemanticProbes;
    }

}
