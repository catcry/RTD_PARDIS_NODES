package com.bonyan.pardis;

public class PardisInputFileBuilder {

    private PardisInputFileService pardisInputFileService;

    PardisInputFileBuilder() {
        pardisInputFileService = new PardisInputFileService();
    }

    public void buildRandomInputFile(String inputFilePath) {
        pardisInputFileService.buildFile(inputFilePath);
    }

    public void buildInputFileWithMsisdnRange(String inputFilePath, int msisdnStartLast4Digits, int msisdnEndLast4Digits) {
        pardisInputFileService.buildFile(inputFilePath, msisdnStartLast4Digits, msisdnEndLast4Digits);
    }

    public void buildInputFileWithRecordCount(String inputFilePath, int recordCount) {
        pardisInputFileService.buildFile(inputFilePath, recordCount);
    }
}
