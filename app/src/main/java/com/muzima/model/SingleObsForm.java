package com.muzima.model;

import com.muzima.api.model.Concept;

import java.util.Date;

public class SingleObsForm {
    private Concept concept;
    private Date date;
    /**
     * Can Be;
     * NUMERIC_TYPE = "Numeric";
     * CODED_TYPE = "Coded";
     * DATETIME_TYPE = "Datetime";
     * DATE_TYPE = "Date";
     *
     */
    private String inputDataType;
    private String inputValue;
    private int readingCount;
    private String inputDateValue;

    public SingleObsForm() {
    }

    public SingleObsForm(Concept concept, Date date, String inputDataType, String inputValue, int readingCount) {
        this.concept = concept;
        this.date = date;
        this.inputDataType = inputDataType;
        this.inputValue = inputValue;
        this.readingCount = readingCount;
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getInputDataType() {
        return inputDataType;
    }

    public void setInputDataType(String inputDataType) {
        this.inputDataType = inputDataType;
    }

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public int getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(int readingCount) {
        this.readingCount = readingCount;
    }

    public String getInputDateValue() {
        return inputDateValue;
    }

    public void setInputDateValue(String inputDateValue) {
        this.inputDateValue = inputDateValue;
    }
}
