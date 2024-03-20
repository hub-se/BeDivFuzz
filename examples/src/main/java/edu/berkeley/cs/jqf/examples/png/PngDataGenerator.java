package edu.berkeley.cs.jqf.examples.png;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class PngDataGenerator{

    // IHDR values
    private byte[] imageWidth = new byte[4];
    private byte[] imageHeight = new byte[4];
    private byte bitsPerChannel, colorType, interlace;

    // image data values
    private int width, height, channels, scanline;

    // chunks
    private boolean PLTEUsed, tRNSUsed;
    private int transparencyMethod;
    private boolean tEXtUsed, zTXtUsed, iTXtUsed;
    private boolean gAMAUsed, cHRMUsed, sRGBUsed;
    private boolean bGKDUsed, pHYsUsed, sBITUsed;
    private int backgroundMethod;

    // debugging
    private final boolean debugging;

    public PngDataGenerator(boolean debugging){
        this.debugging = debugging;
    }

    public byte[] generate(SourceOfRandomness randomness) {

        ByteArrayOutputStream png = new ByteArrayOutputStream();

        resetParameters();

        try {

            initializeParameters(randomness);

            png.write(generateSignature());
            png.write(generateIHDR(randomness));
            if(sBITUsed)
                png.write(generateSBIT(randomness));
            if(sRGBUsed)
                png.write(generateSRGB(randomness));
            if(gAMAUsed)
                png.write(generateGAMA(randomness));
            if(cHRMUsed)
                png.write(generateCHRM(randomness));
            if(PLTEUsed)
                png.write(generatePLTE(randomness));
            if(tRNSUsed)
                png.write(generateTRNS(randomness));
            if(bGKDUsed)
                png.write(generateBKGD(randomness));
            if(pHYsUsed)
                png.write(generatePHYS(randomness));
            if(tEXtUsed)
                for(int i = 0; i < randomness.nextInt(1, 3); i++) {
                    png.write(generateTEXT(randomness));
                }
            if(zTXtUsed)
                for(int i = 0; i < randomness.nextInt(1, 3); i++) {
                    png.write(generateZTXT(randomness));
                }
            png.write(generateIDAT(randomness));
            if(iTXtUsed)
                for(int i = 0; i < randomness.nextInt(1, 3); i++) {
                    png.write(generateITXT(randomness));
                }
            png.write(generateIEND());

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return png.toByteArray();
    }

    private void resetParameters() {
        imageWidth = new byte[4];
        imageHeight = new byte[4];
        bitsPerChannel = 0;
        colorType = 0;
        interlace = 0;

        width = 0;
        height = 0;
        channels = 0;
        scanline = 0;

        PLTEUsed = false;
        tRNSUsed = false;
        transparencyMethod = 0;
        tEXtUsed = false;
        zTXtUsed = false;
        iTXtUsed = false;
        gAMAUsed = false;
        cHRMUsed = false;
        sRGBUsed = false;
        bGKDUsed = false;
        backgroundMethod = 0;
        pHYsUsed = false;
        sBITUsed = false;
    }

    private byte[] generateSignature(){

        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private void initializeParameters(SourceOfRandomness randomness) {

        this.imageWidth = intToByteArray(randomness.nextInt(1, 100));
        this.imageHeight = intToByteArray(randomness.nextInt(1, 100));

        this.interlace = (byte) 0x00;

        initializeRandomColoring(randomness);

        this.tEXtUsed = randomness.nextBoolean();
        this.zTXtUsed = randomness.nextBoolean();
        this.iTXtUsed = randomness.nextBoolean();
        this.gAMAUsed = randomness.nextBoolean();
        this.cHRMUsed = randomness.nextBoolean();
        this.sRGBUsed = randomness.nextBoolean();
        if(sRGBUsed) {
            this.gAMAUsed = true;
            this.cHRMUsed = true;
        }
        this.bGKDUsed = randomness.nextBoolean();
        this.pHYsUsed = randomness.nextBoolean();
        this.sBITUsed = randomness.nextBoolean();

        // DEBUGGING AREA
        /*
        this.initializeRandomColoring(randomness, 4, 1);
        this.tEXtUsed = false;
        this.zTXtUsed = false;
        this.iTXtUsed = false;
        this.gAMAUsed = false;
        this.cHRMUsed = false;
        this.sRGBUsed = false;
        this.PLTEUsed = true;
        this.tRNSUsed = false;
        this.imageHeight = intToByteArray(10);
        this.imageWidth = intToByteArray(10);
        */
        // END OF DEBUGGING AREA

        this.width = ByteBuffer.wrap(imageWidth).getInt();
        this.height = ByteBuffer.wrap(imageHeight).getInt();

    }

    private void initializeRandomColoring(SourceOfRandomness randomness, int colorMethod, int bitDepth) {

        if (colorMethod == -1)
            colorMethod = randomness.nextInt(5);


        switch (colorMethod) {
            case 0: // grayscale
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(5)));
                this.colorType = 0x00;
                this.channels = 1;
                if(randomness.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 1;
                }
                this.backgroundMethod = 1;
                break;
            case 1: // grayscale with alpha
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(3,4)));
                this.colorType = 0x04;
                this.channels = 2;
                this.backgroundMethod = 1;
                break;
            case 2: // true color
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(3,4)));
                this.colorType = 0x02;
                this.channels = 3;
                if(randomness.nextBoolean())
                    PLTEUsed = true;
                if(randomness.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 2;
                }
                this.backgroundMethod = 2;
                break;
            case 3: // true color with alpha
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(3,4)));
                this.colorType = 0x06;
                this.channels = 4;
                if(randomness.nextBoolean())
                    PLTEUsed = true;
                this.backgroundMethod = 2;
                break;
            case 4: // indexed color, palette used
                this.bitsPerChannel = (byte) ((int) Math.pow(2, randomness.nextInt(4)));
                this.colorType = 0x03;
                this.channels = 1;
                this.PLTEUsed = true;
                if(randomness.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 0;
                }
                this.backgroundMethod = 0;
                break;

        }

        if (bitDepth != -1)
            this.bitsPerChannel = (byte) bitDepth;
    }

    private void initializeRandomColoring (SourceOfRandomness randomness) {
        initializeRandomColoring(randomness, -1, -1);
    }

    private byte[] generateIHDR(SourceOfRandomness randomness){

        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();

        try {

            // writes options into the IHDR chunk
            ihdr.write(imageWidth);
            ihdr.write(imageHeight);
            ihdr.write(bitsPerChannel);
            ihdr.write(colorType);
            // compression method is fixed at 0x00
            ihdr.write(0x00);
            // filter methods are always 0 in the IHDR, the difference comes in the image data!
            ihdr.write(0x00);
            ihdr.write(interlace);

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        byte[] ihdrBytes = ChunkBuilder.constructChunk("IHDR".getBytes(), ihdr);

        debugHex("IHDR", ihdrBytes);

        return ihdrBytes;

    }

    private byte[] generateSBIT(SourceOfRandomness randomness) {

        ByteArrayOutputStream sBit = new ByteArrayOutputStream();

        switch (colorType) {
            case 0:
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                break;
            case 2:
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                break;
            case 3:
                sBit.write(randomness.nextInt(1, 8));
                sBit.write(randomness.nextInt(1, 8));
                sBit.write(randomness.nextInt(1, 8));
                break;
            case 4:
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                break;
            case 6:
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                sBit.write(randomness.nextInt(1, bitsPerChannel));
                break;
        }

        return ChunkBuilder.constructChunk("sBIT".getBytes(), sBit);
    }

    private byte[] generateGAMA(SourceOfRandomness randomness) {

        if(sRGBUsed) { // sRGB uses a fixed gAMA value
            return ChunkBuilder.constructChunk("gAMA".getBytes(), intToByteArray(45455));
        }

        byte[] gAMA = intToByteArray(randomness.nextInt(100001));
        return ChunkBuilder.constructChunk("gAMA".getBytes(), gAMA);
    }

    private byte[] generateCHRM(SourceOfRandomness randomness) {

        ByteArrayOutputStream cHRM = new ByteArrayOutputStream();

        try {

            if(sRGBUsed) { // sRGB uses fixed cHRM values
                cHRM.write(intToByteArray(31270)); // white point x
                cHRM.write(intToByteArray(32900)); // white point y
                cHRM.write(intToByteArray(64000)); // red x
                cHRM.write(intToByteArray(33000)); // red y
                cHRM.write(intToByteArray(30000)); // green x
                cHRM.write(intToByteArray(60000)); // green y
                cHRM.write(intToByteArray(15000)); // blue x
                cHRM.write(intToByteArray(6000)); // blue y
            }
            else {
                for(int i = 0; i < 8; i++) {
                    cHRM.write(intToByteArray(randomness.nextInt(100001))); // randomized values
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return ChunkBuilder.constructChunk("cHRM".getBytes(), cHRM);
    }

    private byte[] generateSRGB(SourceOfRandomness randomness) {

        byte[] sRGB = new byte[]{(byte) randomness.nextInt(4)};
        return ChunkBuilder.constructChunk("sRGB".getBytes(), sRGB);

    }
    private byte[] generatePLTE(SourceOfRandomness randomness) {

        ByteArrayOutputStream PLTE = new ByteArrayOutputStream();

        for(int i = 0; i < (int) Math.pow(2, bitsPerChannel); i++) {

            PLTE.write((byte) (randomness.nextInt((int) Math.pow(2, 8)))); // red
            PLTE.write((byte) (randomness.nextInt((int) Math.pow(2, 8)))); // green
            PLTE.write((byte) (randomness.nextInt((int) Math.pow(2, 8)))); // blue
        }

        byte[] PLTEChunk = ChunkBuilder.constructChunk("PLTE".getBytes(), PLTE);

        debugHex("PLTE", PLTEChunk);

        return PLTEChunk;

    }

    private byte[] generateTRNS(SourceOfRandomness randomness) {

        ByteArrayOutputStream tRNS = new ByteArrayOutputStream();

        // dependent on the bit-depth, all irrelevant zeros shall be 0
        // the channel size stays 2 bytes for case 1 and 2
        try {
            switch (transparencyMethod) {
                case 0: // for indexed colors, artificial alpha palette
                    for (int i = 0; i < 256; i++) {
                        tRNS.write((byte) (randomness.nextInt((int) Math.pow(2, 8))));
                    }
                    break;
                case 1: // for grayscale, single alpha value for specified bytes
                    tRNS.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel))));
                    break;
                case 2: // for true color, single alpha value for specified bytes
                    tRNS.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel)))); // red
                    tRNS.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel)))); // green
                    tRNS.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel)))); // blue
                    break;
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("tRNS".getBytes(), tRNS);

    }

    private byte[] generateBKGD(SourceOfRandomness randomness) {

        ByteArrayOutputStream bKGD = new ByteArrayOutputStream();

        try {
            switch (backgroundMethod) {
                case 0: // for indexed colors
                    bKGD.write((byte) (randomness.nextInt((int) Math.pow(2, 8))));
                    break;
                case 1: // for grayscale
                    bKGD.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel))));
                    break;
                case 2: // for true color
                    bKGD.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel)))); // red
                    bKGD.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel)))); // green
                    bKGD.write(int2ToByteArray(randomness.nextInt((int) Math.pow(2, bitsPerChannel)))); // blue
                    break;
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        return ChunkBuilder.constructChunk("bKGD".getBytes(), bKGD);

    }

    private byte[] generatePHYS(SourceOfRandomness randomness) {

        ByteArrayOutputStream pHYs = new ByteArrayOutputStream();

        // specifies the aspect ratio between x and y in pixels
        try {
            pHYs.write(intToByteArray(randomness.nextInt(100))); // x
            pHYs.write(intToByteArray(randomness.nextInt(100))); // y
            pHYs.write((byte) randomness.nextInt(0, 1)); // unknown unit or meter
        }
        catch (IOException e) { e.printStackTrace(); }

        debugHex("pHYs", pHYs.toByteArray());

        return ChunkBuilder.constructChunk("pHYs".getBytes(), pHYs);

    }

    private byte[] generateTEXT(SourceOfRandomness randomness) {

        ByteArrayOutputStream tEXt = new ByteArrayOutputStream();

        // Keyword
        for(int i = 0; i < randomness.nextInt(1, 79); i++) {
            if(randomness.nextBoolean()) {
                tEXt.write((byte) randomness.nextInt(32, 126));
            } else {
                tEXt.write((byte) randomness.nextInt(161, 255));
            }
        }
        // Null separator
        tEXt.write(0x00);
        // Text
        for(int i = 0; i < randomness.nextInt(1, 256); i++) { // 256 (could be maximum chunk size - 80)
            int j = randomness.nextInt(1, 100);
            if(j < 50) {
                tEXt.write((byte) randomness.nextInt(32, 126));
            } else if(j < 99) {
                tEXt.write((byte) randomness.nextInt(161, 255));
            } else {
                tEXt.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }

        byte[] tEXtBytes = ChunkBuilder.constructChunk("tEXt".getBytes(), tEXt);

        debugHex("tEXt", tEXtBytes);
        
        return tEXtBytes;
    }

    private byte[] generateZTXT(SourceOfRandomness randomness) {

        ByteArrayOutputStream zTXt = new ByteArrayOutputStream();
        ByteArrayOutputStream zTXt_text = new ByteArrayOutputStream();

        // Keyword
        for(int i = 0; i < randomness.nextInt(1, 79); i++) {
            if(randomness.nextBoolean()) {
                zTXt.write((byte) randomness.nextInt(32, 126));
            } else {
                zTXt.write((byte) randomness.nextInt(161, 255));
            }
        }
        // Null separator
        zTXt.write(0x00);
        // Compression method
        zTXt.write(0x00);
        // Text
        for(int i = 0; i < randomness.nextInt(1, 256); i++) { // 256 (could be maximum chunk size - 80)
            int j = randomness.nextInt(1, 100);
            if(j < 50) {
                zTXt_text.write((byte) randomness.nextInt(32, 126));
            } else if(j < 99) {
                zTXt_text.write((byte) randomness.nextInt(161, 255));
            } else {
                zTXt_text.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }

        int compressionMethod = 0; // 0 is the only defined compression method

        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, zTXt_text.toByteArray());
        zTXt.write(compressedData, 0, compressedData.length);

        byte[] zTXtBytes = ChunkBuilder.constructChunk("zTXt".getBytes(), zTXt);

        debugHex("zTXt", zTXtBytes);
        
        return zTXtBytes;
    }

    private byte[] generateITXT(SourceOfRandomness randomness) {

        ByteArrayOutputStream iTXt = new ByteArrayOutputStream();
        ByteArrayOutputStream iTXt_text = new ByteArrayOutputStream();

        try {
            // Keyword
            iTXt.write(create_utf8(randomness, randomness.nextInt(1, 79)));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // Null separator
        iTXt.write(0x00);

        // Compression flag (0 for uncompressed, 1 for compressed)
        boolean isCompressed = randomness.nextBoolean();
        iTXt.write((byte) (isCompressed ? 1 : 0));

        // Compression method
        int compressionMethod = 0; // 0 is the only defined compression method
        iTXt.write(0x00);

        try{
            // Language tag
            String[] languages = {"cn", "en-uk", "no-bok", "x-klingon"}; // hardcoded
            if(randomness.nextBoolean()) {
                iTXt.write(languages[randomness.nextInt(0, 3)].getBytes());
            }
            // Null separator
            iTXt.write(0x00);

            // Translated keyword
            iTXt.write(create_utf8(randomness, randomness.nextInt(0, 100))); // this keyword can be longer than 79 bytes
            // Null separator
            iTXt.write(0x00);

            // Text
            iTXt_text.write(create_utf8(randomness, randomness.nextInt(0, 256))); // 256 (could be maximum chunk size - other data bytes of this chunk)
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        if(isCompressed) {

            byte[] compressedData = ChunkBuilder.compressData(compressionMethod, iTXt_text.toByteArray());
            iTXt.write(compressedData, 0, compressedData.length);

        } else {
            try{
                iTXt.write(iTXt_text.toByteArray());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] iTXtBytes = ChunkBuilder.constructChunk("iTXt".getBytes(), iTXt);

        debugHex("iTXt", iTXtBytes);
        
        return iTXtBytes;
    }

    private byte[] generateIDAT(SourceOfRandomness randomness) {

        byte[] filteredData = generateFilteredData(randomness);

        debugHex("filtered data", filteredData);

        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        int compressionMethod = randomness.nextInt(-1,9);
        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, filteredData);
        idat.write(compressedData, 0, compressedData.length);

        byte[] idatChunk = ChunkBuilder.constructChunk("IDAT".getBytes(), idat);

        debugHex("IDAT", idatChunk);

        return idatChunk;
    }

    private byte[] generateFilteredData(SourceOfRandomness randomness){

        // the scanline indicates the length of one horizontal line in the image (filter byte included)

        float channelSize = (float) bitsPerChannel / 8;
        scanline = (int) Math.ceil(width * channels * channelSize) + 1;
        byte[] imageData = new byte[height * scanline];

        for (int y = 0; y < height; y++) {

            // each line can opt for a different filter method
            int filterMethod = (byte) randomness.nextInt(5);

            //filterMethod = 0;
            // the first byte of each scanline defines the filter method
            imageData[y * scanline] = (byte) filterMethod;

            for (int x = 1; x < scanline; x++) {

                // the position of each byte in the image data
                int position = y * scanline + x;

                // each byte is randomized, based on the channelSize (bit-depth)
                // ... multiple channels or pixel can be in one byte
                byte imageByte = (byte) (randomness.nextInt((int) Math.pow(2, 8)));
                imageData[position] = imageByte;

                // the filter is added onto each byte based on the filter method
                byte filteredImageByte = Filter.addFilter(filterMethod, imageData, position, scanline, channels);
                imageData[position] = filteredImageByte;
            }
        }

        return imageData;

    }

    private byte[] generateIEND(){

        return new byte[]{0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
                0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82};

    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02X ", b));
        return sb.toString();
    }

    public void debugHex(String name, byte[] bytes) {
        if(debugging)
            System.out.println(name + ": " + byteArrayToHex(bytes));
    }

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] int2ToByteArray(int value) {
        return ByteBuffer.allocate(2).putShort((short) value).array();
    }

    public byte[] create_utf8(SourceOfRandomness randomness, int max_byte_number) {
        String str = "";
        for(int i = 0; i < max_byte_number - 1; i++) {
            str = str + randomness.nextChar((char) 0x0001, (char) 0x10FFFF); // uses maximum range of Unicode
        }
        byte[] bytes = Arrays.copyOfRange(str.getBytes(StandardCharsets.UTF_8), 0, str.length());
        if(bytes.length > 0 && bytes.length < 5) {
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) randomness.nextInt(1, 127);
            }
            return bytes;
        }
        return utf8_correction(bytes);
    }

    public byte[] utf8_correction(byte[] b) { // removes wrong bytes from the end
        if(b.length == 0) {return b;}
        if((b[b.length-1] >> 7) == 0) {
            return b;
        }
        if((b[b.length-1] >> 6) == -1) {
            return Arrays.copyOfRange(b, 0, b.length - 1);
        }
        return utf8_correction(Arrays.copyOfRange(b, 0, b.length - 1));
    }

    public static void main(String[] args) {

        for(int i = 0; i < 1; i++) {

            PngDataGenerator gen = new PngDataGenerator(true);
            SourceOfRandomness randomness = new SourceOfRandomness(new Random());

            byte[] png = gen.generate(randomness);
            gen.debugHex("Png", png);

            try {

                FileOutputStream fos = new FileOutputStream("Debugging_Png.png");
                fos.write(png);
                fos.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}