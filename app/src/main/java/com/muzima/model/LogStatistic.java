package com.muzima.model;

public class LogStatistic {
    private String tag;
    private String date;
    private String providerId;
    private String details;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("tag").append(" = ").append(tag)
                .append("date").append(" = ").append(date)
                .append("provider").append(" = ").append(providerId)
                .append("details").append(" = ").append(details);
        return stringBuilder.toString();
    }
}
