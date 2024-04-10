package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.Collection;

public interface SplitRandom {

    /**
     * Methods to access one of the underlying random sequences.
     */

    SourceOfRandomness getStructureDelegate();
    SourceOfRandomness getValueDelegate();

    /**
     * Methods to perform structure random choices.
     */

    byte nextStructureByte(byte min, byte max);

    void nextStructureBytes(byte[] bytes);

    double nextStructureDouble();

    double nextStructureDouble(double min, double max);

    float nextStructureFloat();

    float nextStructureFloat(float min, float max);

    short nextStructureShort(short min, short max);

    char nextStructureChar(char min, char max);

    int nextStructureInt();

    int nextStructureInt(int n);

    int nextStructureInt(int min, int max);

    boolean nextStructureBoolean();

    long nextStructureLong();

    long nextStructureLong(long min, long max);

    <T> T chooseStructure(Collection<T> items);

    <T> T chooseStructure(T[] items);


    /**
    * Methods to perform value random choices.
    */

    byte nextValueByte(byte min, byte max);

    byte[] nextValueBytes(int count);

    double nextValueDouble();

    double nextValueDouble(double min, double max);

    float nextValueFloat();

    float nextValueFloat(float min, float max);

    short nextValueShort(short min, short max);

    char nextValueChar(char min, char max);

    int nextValueInt();

    int nextValueInt(int n);

    int nextValueInt(int min, int max);

    boolean nextValueBoolean();

    long nextValueLong();

    long nextValueLong(long min, long max);

    <T> T chooseValue(Collection<T> items);

    <T> T chooseValue(T[] items);
 }
