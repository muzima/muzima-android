package com.muzima.model;

public class ProviderReportStatistic {
    private int achievement;
    private String achievementId;
    private float achievementGroupAverage;
    private int expectedAchievement;
    private String providerId;
    private String providerName;
    private String statisticTitle;
    private String statisticHint;

    public int getAchievement() {
        return achievement;
    }

    public void setAchievement(int achievement) {
        this.achievement = achievement;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }

    public float getAchievementGroupAverage() {
        return achievementGroupAverage;
    }

    public void setAchievementGroupAverage(float achievementGroupAverage) {
        this.achievementGroupAverage = achievementGroupAverage;
    }

    public int getExpectedAchievement() {
        return expectedAchievement;
    }

    public void setExpectedAchievement(int expectedAchievement) {
        this.expectedAchievement = expectedAchievement;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getStatisticTitle() {
        return statisticTitle;
    }

    public void setStatisticTitle(String statisticTitle) {
        this.statisticTitle = statisticTitle;
    }

    public String getStatisticHint() {
        return statisticHint;
    }

    public void setStatisticHint(String statisticHint) {
        this.statisticHint = statisticHint;
    }
}
