package edu.berkeley.cs.jqf.examples.png;

public class Filter {

    public static byte addFilter(int filterMethod, byte[] imageData, int position, int scanline, int channels){
        switch (filterMethod) {
            case 1:
                return subFilter(imageData, position, scanline, channels);
            case 2:
                return upFilter(imageData, position, scanline);
            case 3:
                return averageFilter(imageData, position, scanline, channels);
            case 4:
                return paethFilter(imageData, position, scanline, channels);
            default:
                return imageData[position];
        }
    }

    private static byte subFilter(byte[] imageData, int position, int scanline, int channels) {

        byte filteredImageByte;

        // first pixel of each scanline is ignored
        if(position % scanline < channels) {
            filteredImageByte = imageData[position];
        }
        else {
            int sub = imageData[position] - imageData[position - channels];
            int mod = Integer.remainderUnsigned(sub, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private static byte upFilter(byte[] imageData, int position, int scanline) {

        byte filteredImageByte;

        // first scanline is ignored
        if(position < scanline) {
            filteredImageByte =  imageData[position];
        }
        else {
            int sub = imageData[position] - imageData[position - scanline];
            int mod = Integer.remainderUnsigned(sub, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private static byte averageFilter(byte[] imageData, int position, int scanline, int channels) {

        byte filteredImageByte;

        // first pixel of each scanline and the first scanline itself are ignored
        if(position < scanline || position % scanline < channels) {
            filteredImageByte = imageData[position];
        }
        else {
            int left = imageData[position - channels];
            int up = imageData[position - scanline];
            int subAverage = imageData[position] - (left + up) / 2;
            int mod = Integer.remainderUnsigned(subAverage, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private static byte paethFilter(byte[] imageData, int position, int scanline, int channels) {

        byte filteredImageByte;

        // first pixel of each scanline and the first scanline itself are ignored
        if(position < scanline || position % scanline < channels) {
            filteredImageByte = imageData[position];
        }
        else {
            int left = imageData[position - channels];
            int above = imageData[position - scanline];
            int upperLeft = imageData[position - scanline - channels];
            int subPaeth = imageData[position] - PaethPredictor(left, above, upperLeft);
            int mod = Integer.remainderUnsigned(subPaeth, 256);
            filteredImageByte = (byte) mod;
        }

        return filteredImageByte;

    }

    private static int PaethPredictor(int left, int above, int upperLeft) {

        // paeth predictor is an algorithm used for the paeth filter

        int p = left + above - upperLeft;
        int pLeft = Math.abs(p - left);
        int pAbove = Math.abs(p - above);
        int pUpperLeft = Math.abs(p - upperLeft);
        if(pLeft <= pAbove && pLeft <= pUpperLeft) {
            return left;
        }
        else if(pAbove <= pUpperLeft) {
            return above;
        }
        return upperLeft;
    }

    /*
    PSEUDO-CODE by libpng

    function PaethPredictor (a, b, c)
       begin
            ; a = left, b = above, c = upper left
            p := a + b - c        ; initial estimate
            pa := abs(p - a)      ; distances to a, b, c
            pb := abs(p - b)
            pc := abs(p - c)
            ; return nearest of a,b,c,
            ; breaking ties in order a,b,c.
            if pa <= pb AND pa <= pc then return a
            else if pb <= pc then return b
            else return c
       end
     */

}
