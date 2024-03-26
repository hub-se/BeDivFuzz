package edu.berkeley.cs.jqf.examples.png;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PngDataGenerator{
    // IHDR values
    private byte[] imageWidth = new byte[4];
    private byte[] imageHeight = new byte[4];
    private byte bitsPerChannel;
    private byte colorType;
    private byte interlace;

    // Image data values
    private int width;
    private int  height;
    private int channels;
    private int scanline;

    // Chunks
    private boolean PLTEUsed;
    private boolean tRNSUsed;
    private int transparencyMethod;
    private boolean tEXtUsed;
    private boolean zTXtUsed;
    private boolean iTXtUsed;
    private boolean gAMAUsed;
    private boolean cHRMUsed;
    private boolean sRGBUsed;
    private boolean bGKDUsed;
    private boolean pHYsUsed;
    private boolean sBITUsed;
    private int backgroundMethod;

    public PngDataGenerator(){}

    public byte[] generate(SourceOfRandomness random) {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        resetParameters();

        try {
            initializeParameters(random);

            png.write(generateSignature());
            png.write(generateIHDR(random));
            if(sBITUsed)
                png.write(generateSBIT(random));
            if(sRGBUsed)
                png.write(generateSRGB(random));
            if(gAMAUsed)
                png.write(generateGAMA(random));
            if(cHRMUsed)
                png.write(generateCHRM(random));
            if(PLTEUsed)
                png.write(generatePLTE(random));
            if(tRNSUsed)
                png.write(generateTRNS(random));
            if(bGKDUsed)
                png.write(generateBKGD(random));
            if(pHYsUsed)
                png.write(generatePHYS(random));
            if(tEXtUsed)
                for(int i = 0; i < random.nextInt(1, 3); i++) {
                    png.write(generateTEXT(random));
                }
            if(zTXtUsed)
                for(int i = 0; i < random.nextInt(1, 3); i++) {
                    png.write(generateZTXT(random));
                }
            png.write(generateIDAT(random));
            if(iTXtUsed)
                for(int i = 0; i < random.nextInt(1, 3); i++) {
                    png.write(generateITXT(random));
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

    private void initializeParameters(SourceOfRandomness random) {
        this.imageWidth = intToByteArray(random.nextInt(1, 100));
        this.imageHeight = intToByteArray(random.nextInt(1, 100));
        this.interlace = (byte) 0x00;

        initializeRandomColoring(random);

        this.tEXtUsed = random.nextBoolean();
        this.zTXtUsed = random.nextBoolean();
        this.iTXtUsed = random.nextBoolean();
        this.gAMAUsed = random.nextBoolean();
        this.cHRMUsed = random.nextBoolean();
        this.sRGBUsed = random.nextBoolean();
        if(sRGBUsed) {
            this.gAMAUsed = true;
            this.cHRMUsed = true;
        }
        this.bGKDUsed = random.nextBoolean();
        this.pHYsUsed = random.nextBoolean();
        this.sBITUsed = random.nextBoolean();

        this.width = ByteBuffer.wrap(imageWidth).getInt();
        this.height = ByteBuffer.wrap(imageHeight).getInt();
    }

    private void initializeRandomColoring(SourceOfRandomness random, int colorMethod, int bitDepth) {
        if (colorMethod == -1) {
            colorMethod = random.nextInt(5);
        }

        switch (colorMethod) {
            case 0: // grayscale
                this.bitsPerChannel = (byte) ((int) Math.pow(2, random.nextInt(5)));
                this.colorType = 0x00;
                this.channels = 1;
                if(random.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 1;
                }
                this.backgroundMethod = 1;
                break;
            case 1: // grayscale with alpha
                this.bitsPerChannel = (byte) ((int) Math.pow(2, random.nextInt(3,4)));
                this.colorType = 0x04;
                this.channels = 2;
                this.backgroundMethod = 1;
                break;
            case 2: // true color
                this.bitsPerChannel = (byte) ((int) Math.pow(2, random.nextInt(3,4)));
                this.colorType = 0x02;
                this.channels = 3;
                if(random.nextBoolean())
                    PLTEUsed = true;
                if(random.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 2;
                }
                this.backgroundMethod = 2;
                break;
            case 3: // true color with alpha
                this.bitsPerChannel = (byte) ((int) Math.pow(2, random.nextInt(3,4)));
                this.colorType = 0x06;
                this.channels = 4;
                if(random.nextBoolean())
                    PLTEUsed = true;
                this.backgroundMethod = 2;
                break;
            case 4: // indexed color, palette used
                this.bitsPerChannel = (byte) ((int) Math.pow(2, random.nextInt(4)));
                this.colorType = 0x03;
                this.channels = 1;
                this.PLTEUsed = true;
                if(random.nextBoolean()) {
                    tRNSUsed = true;
                    transparencyMethod = 0;
                }
                this.backgroundMethod = 0;
                break;
        }

        if (bitDepth != -1) {
            this.bitsPerChannel = (byte) bitDepth;
        }
    }

    private void initializeRandomColoring (SourceOfRandomness random) {
        initializeRandomColoring(random, -1, -1);
    }

    private byte[] generateIHDR(SourceOfRandomness random){
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
        return ChunkBuilder.constructChunk("IHDR".getBytes(), ihdr);
    }

    private byte[] generateSBIT(SourceOfRandomness random) {
        ByteArrayOutputStream sBit = new ByteArrayOutputStream();

        switch (colorType) {
            case 0:
                sBit.write(random.nextInt(1, bitsPerChannel));
                break;
            case 2:
                sBit.write(random.nextInt(1, bitsPerChannel));
                sBit.write(random.nextInt(1, bitsPerChannel));
                sBit.write(random.nextInt(1, bitsPerChannel));
                break;
            case 3:
                sBit.write(random.nextInt(1, 8));
                sBit.write(random.nextInt(1, 8));
                sBit.write(random.nextInt(1, 8));
                break;
            case 4:
                sBit.write(random.nextInt(1, bitsPerChannel));
                sBit.write(random.nextInt(1, bitsPerChannel));
                break;
            case 6:
                sBit.write(random.nextInt(1, bitsPerChannel));
                sBit.write(random.nextInt(1, bitsPerChannel));
                sBit.write(random.nextInt(1, bitsPerChannel));
                sBit.write(random.nextInt(1, bitsPerChannel));
                break;
        }
        return ChunkBuilder.constructChunk("sBIT".getBytes(), sBit);
    }

    private byte[] generateGAMA(SourceOfRandomness random) {
        if(sRGBUsed) { // sRGB uses a fixed gAMA value
            return ChunkBuilder.constructChunk("gAMA".getBytes(), intToByteArray(45455));
        }
        byte[] gAMA = intToByteArray(random.nextInt(100001));
        return ChunkBuilder.constructChunk("gAMA".getBytes(), gAMA);
    }

    private byte[] generateCHRM(SourceOfRandomness random) {
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
                    cHRM.write(intToByteArray(random.nextInt(100001))); // randomized values
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return ChunkBuilder.constructChunk("cHRM".getBytes(), cHRM);
    }

    private byte[] generateSRGB(SourceOfRandomness random) {
        byte[] sRGB = new byte[]{(byte) random.nextInt(4)};
        return ChunkBuilder.constructChunk("sRGB".getBytes(), sRGB);
    }

    private byte[] generatePLTE(SourceOfRandomness random) {
        ByteArrayOutputStream PLTE = new ByteArrayOutputStream();
        for(int i = 0; i < (int) Math.pow(2, bitsPerChannel); i++) {
            PLTE.write((byte) (random.nextInt((int) Math.pow(2, 8)))); // red
            PLTE.write((byte) (random.nextInt((int) Math.pow(2, 8)))); // green
            PLTE.write((byte) (random.nextInt((int) Math.pow(2, 8)))); // blue
        }
        return ChunkBuilder.constructChunk("PLTE".getBytes(), PLTE);
    }

    private byte[] generateTRNS(SourceOfRandomness random) {
        ByteArrayOutputStream tRNS = new ByteArrayOutputStream();

        // dependent on the bit-depth, all irrelevant zeros shall be 0
        // the channel size stays 2 bytes for case 1 and 2
        try {
            switch (transparencyMethod) {
                case 0: // for indexed colors, artificial alpha palette
                    for (int i = 0; i < 256; i++) {
                        tRNS.write((byte) (random.nextInt((int) Math.pow(2, 8))));
                    }
                    break;
                case 1: // for grayscale, single alpha value for specified bytes
                    tRNS.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel))));
                    break;
                case 2: // for true color, single alpha value for specified bytes
                    tRNS.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel)))); // red
                    tRNS.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel)))); // green
                    tRNS.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel)))); // blue
                    break;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return ChunkBuilder.constructChunk("tRNS".getBytes(), tRNS);
    }

    private byte[] generateBKGD(SourceOfRandomness random) {
        ByteArrayOutputStream bKGD = new ByteArrayOutputStream();
        try {
            switch (backgroundMethod) {
                case 0: // for indexed colors
                    bKGD.write((byte) (random.nextInt((int) Math.pow(2, 8))));
                    break;
                case 1: // for grayscale
                    bKGD.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel))));
                    break;
                case 2: // for true color
                    bKGD.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel)))); // red
                    bKGD.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel)))); // green
                    bKGD.write(int2ToByteArray(random.nextInt((int) Math.pow(2, bitsPerChannel)))); // blue
                    break;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return ChunkBuilder.constructChunk("bKGD".getBytes(), bKGD);
    }

    private byte[] generatePHYS(SourceOfRandomness random) {
        ByteArrayOutputStream pHYs = new ByteArrayOutputStream();
        // specifies the aspect ratio between x and y in pixels
        try {
            pHYs.write(intToByteArray(random.nextInt(100))); // x
            pHYs.write(intToByteArray(random.nextInt(100))); // y
            pHYs.write((byte) random.nextInt(0, 1)); // unknown unit or meter
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return ChunkBuilder.constructChunk("pHYs".getBytes(), pHYs);
    }

    private byte[] generateTEXT(SourceOfRandomness random) {
        ByteArrayOutputStream tEXt = new ByteArrayOutputStream();

        // Keyword
        for(int i = 0; i < random.nextInt(1, 79); i++) {
            if(random.nextBoolean()) {
                tEXt.write((byte) random.nextInt(32, 126));
            } else {
                tEXt.write((byte) random.nextInt(161, 255));
            }
        }

        // Null separator
        tEXt.write(0x00);
        // Text
        for(int i = 0; i < random.nextInt(1, 256); i++) { // 256 (could be maximum chunk size - 80)
            int j = random.nextInt(1, 100);
            if(j < 50) {
                tEXt.write((byte) random.nextInt(32, 126));
            } else if(j < 99) {
                tEXt.write((byte) random.nextInt(161, 255));
            } else {
                tEXt.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }
        return ChunkBuilder.constructChunk("tEXt".getBytes(), tEXt);
    }

    private byte[] generateZTXT(SourceOfRandomness random) {
        ByteArrayOutputStream zTXt = new ByteArrayOutputStream();
        ByteArrayOutputStream zTXt_text = new ByteArrayOutputStream();

        // Keyword
        for(int i = 0; i < random.nextInt(1, 79); i++) {
            if(random.nextBoolean()) {
                zTXt.write((byte) random.nextInt(32, 126));
            } else {
                zTXt.write((byte) random.nextInt(161, 255));
            }
        }

        // Null separator
        zTXt.write(0x00);
        // Compression method
        zTXt.write(0x00);
        // Text
        for(int i = 0; i < random.nextInt(1, 256); i++) { // 256 (could be maximum chunk size - 80)
            int j = random.nextInt(1, 100);
            if(j < 50) {
                zTXt_text.write((byte) random.nextInt(32, 126));
            } else if(j < 99) {
                zTXt_text.write((byte) random.nextInt(161, 255));
            } else {
                zTXt_text.write((byte) 0x0A); // newline (with a probability of 2%)
            }
        }

        int compressionMethod = 0; // 0 is the only defined compression method

        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, zTXt_text.toByteArray());
        zTXt.write(compressedData, 0, compressedData.length);
        return ChunkBuilder.constructChunk("zTXt".getBytes(), zTXt);
    }

    private byte[] generateITXT(SourceOfRandomness random) {
        ByteArrayOutputStream iTXt = new ByteArrayOutputStream();
        ByteArrayOutputStream iTXt_text = new ByteArrayOutputStream();

        try {
            // Keyword
            iTXt.write(create_utf8(random, random.nextInt(1, 79)));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Null separator
        iTXt.write(0x00);

        // Compression flag (0 for uncompressed, 1 for compressed)
        boolean isCompressed = random.nextBoolean();
        iTXt.write((byte) (isCompressed ? 1 : 0));

        // Compression method
        int compressionMethod = 0; // 0 is the only defined compression method
        iTXt.write(0x00);

        try{
            // Language tag
            String[] languages = {"cn", "en-uk", "no-bok", "x-klingon"}; // hardcoded
            if(random.nextBoolean()) {
                iTXt.write(languages[random.nextInt(0, 3)].getBytes());
            }
            // Null separator
            iTXt.write(0x00);

            // Translated keyword
            iTXt.write(create_utf8(random, random.nextInt(0, 100))); // this keyword can be longer than 79 bytes
            // Null separator
            iTXt.write(0x00);

            // Text
            iTXt_text.write(create_utf8(random, random.nextInt(0, 256))); // 256 (could be maximum chunk size - other data bytes of this chunk)
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
        return ChunkBuilder.constructChunk("iTXt".getBytes(), iTXt);
    }

    private byte[] generateIDAT(SourceOfRandomness random) {
        byte[] filteredData = generateFilteredData(random);
        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        int compressionMethod = random.nextInt(-1,9);
        byte[] compressedData = ChunkBuilder.compressData(compressionMethod, filteredData);
        idat.write(compressedData, 0, compressedData.length);
        return ChunkBuilder.constructChunk("IDAT".getBytes(), idat);
    }

    private byte[] generateFilteredData(SourceOfRandomness random){
        // the scanline indicates the length of one horizontal line in the image (filter byte included)
        float channelSize = (float) bitsPerChannel / 8;
        scanline = (int) Math.ceil(width * channels * channelSize) + 1;
        byte[] imageData = new byte[height * scanline];

        for (int y = 0; y < height; y++) {

            // each line can opt for a different filter method
            int filterMethod = (byte) random.nextInt(5);

            //filterMethod = 0;
            // the first byte of each scanline defines the filter method
            imageData[y * scanline] = (byte) filterMethod;

            for (int x = 1; x < scanline; x++) {

                // the position of each byte in the image data
                int position = y * scanline + x;

                // each byte is randomized, based on the channelSize (bit-depth)
                // ... multiple channels or pixel can be in one byte
                byte imageByte = (byte) (random.nextInt((int) Math.pow(2, 8)));
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

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] int2ToByteArray(int value) {
        return ByteBuffer.allocate(2).putShort((short) value).array();
    }

    public byte[] create_utf8(SourceOfRandomness random, int max_byte_number) {
        String str = "";
        for(int i = 0; i < max_byte_number - 1; i++) {
            str = str + random.nextChar((char) 0x0001, (char) 0x10FFFF); // uses maximum range of Unicode
        }
        byte[] bytes = Arrays.copyOfRange(str.getBytes(StandardCharsets.UTF_8), 0, str.length());
        if(bytes.length > 0 && bytes.length < 5) {
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) random.nextInt(1, 127);
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

}