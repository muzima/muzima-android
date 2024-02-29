package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.HTCPerson;
import com.muzima.api.model.MuzimaHtcForm;
import com.muzima.api.service.HTCPersonService;
import com.muzima.api.service.MuzimaHtcFormService;

import java.io.IOException;

/**
 * @author Jose Julai Ritsure
 */
public class MuzimaHTCFormController {
    private HTCPersonService htcPersonService;
    private final MuzimaHtcFormService muzimaHtcFormService;
    private HTCPerson htcPerson;
    public MuzimaHTCFormController(HTCPersonService htcPersonService, MuzimaHtcFormService muzimaHtcFormService, HTCPerson htcPerson) {
        this.htcPersonService = htcPersonService;
        this.muzimaHtcFormService = muzimaHtcFormService;
        this.htcPerson = htcPerson;
    }
    public MuzimaHTCFormController(MuzimaHtcFormService muzimaHtcFormService) {
        this.muzimaHtcFormService = muzimaHtcFormService;
    }

    public MuzimaHtcForm getHTCForm(String uuid) {
        try {
            MuzimaHtcForm muzimaHtcForm = this.muzimaHtcFormService.getHTCFormByUuid(uuid);
            return muzimaHtcForm;
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while getting htc form with uuid : " + uuid, e);
        }
        return null;
    }

    public void saveHTCForm(MuzimaHtcForm muzimaHtcForm) throws MuzimaHTCFormSaveException {
        try {
            this.muzimaHtcFormService.saveMuzimaHtc(muzimaHtcForm);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving htc form : " + muzimaHtcForm.getUuid(), e);
            throw new MuzimaHTCFormController.MuzimaHTCFormSaveException(e);
        }
    }

    public static class MuzimaHTCFormSaveException extends Throwable {
        public MuzimaHTCFormSaveException(Throwable throwable) {
            super(throwable);
        }
    }
}
