package com.bonyan.pardis;

import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("please pass file path through command line argument");
        }
        String filepath;
        int recordCount;
        int startMsisdnLast4Digits;
        int endMsisdnLast4Digits;
        PardisInputFileBuilder pardisInputFileBuilder = new PardisInputFileBuilder();
        if (args.length == 1) {
            filepath = args[0];
            pardisInputFileBuilder.buildRandomInputFile(filepath);
        } else if (args.length == 2) {
            filepath = args[0];
            recordCount = Integer.parseInt(args[1]);
            pardisInputFileBuilder.buildInputFileWithRecordCount(filepath, recordCount);
        } else if (args.length == 3) {
            filepath = args[0];
            startMsisdnLast4Digits = Integer.parseInt(args[1]);
            endMsisdnLast4Digits = Integer.parseInt(args[2]);
            pardisInputFileBuilder.buildInputFileWithMsisdnRange(filepath, startMsisdnLast4Digits, endMsisdnLast4Digits);
        }
    }
}
