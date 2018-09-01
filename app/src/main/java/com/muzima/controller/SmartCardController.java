package com.muzima.controller;

import com.muzima.api.model.SmartCardRecord;
import com.muzima.api.service.SmartCardRecordService;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SmartCardController {

    private final SmartCardRecordService smartCardRecordService;

    public SmartCardController(SmartCardRecordService smartCardRecordService){
        this.smartCardRecordService = smartCardRecordService;
    }

    public void saveSmartCardRecord(SmartCardRecord smartCardRecord) throws SmartCardRecordSaveException {
        try {
            smartCardRecordService.saveSmartCardRecord(smartCardRecord);
        } catch (IOException e) {
            throw new SmartCardRecordSaveException(e);
        }
    }

    public void updateSmartCardRecord(SmartCardRecord smartCardRecord) throws SmartCardRecordSaveException {
        try {
            smartCardRecordService.updateSmartCardRecord(smartCardRecord);
        } catch (IOException e) {
            throw new SmartCardRecordSaveException(e);
        }
    }

    public SmartCardRecord getSmartCardRecordByUuid(String uuid) throws SmartCardRecordFetchException {
        try {
            return smartCardRecordService.getSmartCardRecordByUuid(uuid);
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }

    public SmartCardRecord getSmartCardRecordByPersonUuid(String personUuid) throws SmartCardRecordFetchException {
        try {
            return smartCardRecordService.getSmartCardRecordByPersonUuid(personUuid);
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }

    private List<SmartCardRecord> getAllSmartCardRecords() throws SmartCardRecordFetchException {
        boolean isSuccess = false;
        try {
            return smartCardRecordService.getAllSmartCardRecords();
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }

    public List<SmartCardRecord> getSmartCardRecordWithNonUploadedData() throws SmartCardRecordFetchException {
        List<SmartCardRecord> smartCardRecords = getAllSmartCardRecords();
        List<SmartCardRecord> smartCardRecordWithNonUploadedData = new ArrayList<>();
        for(SmartCardRecord smartCardRecord : smartCardRecords){
            if(!StringUtils.isEmpty(smartCardRecord.getEncryptedPayload())) {
                smartCardRecordWithNonUploadedData.add(smartCardRecord);
            }
        }
        return smartCardRecordWithNonUploadedData;
    }

    public boolean syncSmartCardRecord(SmartCardRecord smartCardRecord) throws SmartCardRecordFetchException {
        try {
            return smartCardRecordService.syncSmartCardRecord(smartCardRecord);
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }


    public static class SmartCardRecordSaveException extends Throwable {
        SmartCardRecordSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class SmartCardRecordFetchException extends Throwable {
        SmartCardRecordFetchException(Throwable throwable) {
            super(throwable);
        }
    }
}
