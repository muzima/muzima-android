package com.muzima.model;

public class ProviderAchievementStatistic {
    private int achievement;
    private float achievementGroupAverage;
    private int expectedAchievement;
    private String providerId;
    private String statisticTitle;
    private String statisticHint;

    public int getAchievement() {
        return achievement;
    }

    public void setAchievement(int achievement) {
        this.achievement = achievement;
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
