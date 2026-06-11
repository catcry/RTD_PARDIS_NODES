package com.bonyan.pardis;



import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PardisInputFileService {

    private List<String> contentIds;
    private List<String> actionIds;
    private Random random = new Random();
    private BufferedWriter bufferedWriter;
    public static final int MAX_CONTENT_LIST_SIZE = 10;

    public PardisInputFileService() {
        this.contentIds = new ArrayList<>();
        this.actionIds = new ArrayList<>();
        for (int i = 1; i <= MAX_CONTENT_LIST_SIZE; i++) {
            contentIds.add("general_loan_content_" + i);
            actionIds.add("general_loan_action_" + i);
        }
    }

    public void buildFile(String filePath) {
        try {
            createInputFile(filePath);
            createRecords();
            closeFile();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void buildFile(String filePath,int recordCount) {
        try {
            createInputFile(filePath);
            createRecords(recordCount);
            closeFile();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void buildFile(String filePath, int startLast4Digits, int endLast4Digits) {
        try {
            createInputFile(filePath);
            createRecordsInRange(startLast4Digits, endLast4Digits);
            closeFile();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void createInputFile(String filePath) {
        try {
            bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeFile() {
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addRandomRecord(String msisdn) throws IllegalArgumentException, IOException {
        PardisInputFileEntity pardisInputFileEntity = new PardisInputFileEntity();
        String[] randomContentAndAction = getRandomContentAndAction();
        pardisInputFileEntity.setMsisdn(msisdn).setRetryCount("0")
                .setContentId(randomContentAndAction[0]).setActionId(randomContentAndAction[1]);
        bufferedWriter.write(pardisInputFileEntity.getRecordContent());
    }

//  create records from msisdn 09191231000 to 09191232000
    private void createRecords() throws IllegalArgumentException, IOException {
        createRecordsInRange(1000,2000);
    }

//  create records from msisdn 09191231000 to 0919123'(1000+recordCount)'
    private void createRecords(int recordCount) throws IllegalArgumentException, IOException {
        createRecordsInRange(1000, (1000 + recordCount));
    }

//  create records from msisdn 0919123'startLast4Digits' to 0919123'endLast4Digits'
    private void createRecordsInRange(int startLast4Digits, int endLast4Digits) throws IllegalArgumentException, IOException {
        if (startLast4Digits > endLast4Digits){
            throw new IllegalArgumentException("startLast4Digits must be less than endLast4Digits");
        }
        for (int i = startLast4Digits; i < endLast4Digits; i++) {
            addRandomRecord("0919123" + i);
        }
    }

    private String[] getRandomContentAndAction() {
        int randomNumber = this.random.nextInt(MAX_CONTENT_LIST_SIZE);
        return new String[]{contentIds.get(randomNumber), actionIds.get(randomNumber)};
    }
}
