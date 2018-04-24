package com.muzima.controller;

import com.muzima.api.model.SmartCardRecord;
import com.muzima.api.service.SmartCardRecordService;

import java.io.IOException;
import java.util.List;

public class SmartCardController {
    public static final String TAG ="SmartCardController";

    SmartCardRecordService smartCardRecordService;

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

    public SmartCardRecord getSmartCardRecordByPersonUuid(String patientUuid) throws SmartCardRecordFetchException {
        try {
            return smartCardRecordService.getSmartCardRecordByPersonUuid(patientUuid);
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }

    public boolean syncEncryptedSmartCardRecordsToServer() throws SmartCardRecordFetchException {
        try {
            boolean isSuccess = false;
            List<SmartCardRecord> smartCardRecords = smartCardRecordService.getAllSmartCardRecords();
            for(SmartCardRecord smartCardRecord : smartCardRecords){
                isSuccess = syncSmartCardRecord(smartCardRecord);
            }
            return isSuccess;
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }

    public boolean syncSmartCardRecord(SmartCardRecord smartCardRecord) throws SmartCardRecordFetchException {
        try {
            return smartCardRecordService.syncSmartCardRecord(smartCardRecord);
        } catch (IOException e) {
            throw new SmartCardRecordFetchException(e);
        }
    }


    public static class SmartCardRecordSaveException extends Throwable {
        public SmartCardRecordSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class SmartCardRecordFetchException extends Throwable {
        public SmartCardRecordFetchException(Throwable throwable) {
            super(throwable);
        }
    }
}
