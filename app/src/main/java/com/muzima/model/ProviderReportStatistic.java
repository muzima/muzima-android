/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model;

import java.util.HashMap;
import java.util.Map;

public class ProviderReportStatistic implements Comparable{
    private int achievement;
    private String achievementId;
    private float achievementGroupAverage;
    private int expectedAchievement;
    private Map<String,Integer> scoreMap;
    private String providerId;
    private String providerName;
    private String statisticTitle;
    private String statisticHint;
    private String summaryColorCode;
    private int leaderboardColor;
    private String  startDate;
    private String  endDate;
    private String abbreviation;
    private boolean isVisible = true;

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

    public Map<String, Integer> getScoreMap() {
        if(scoreMap == null)
            scoreMap = new HashMap<>();
        return scoreMap;
    }

    public void setScoreMap(Map<String, Integer> scoreMap) {
        this.scoreMap = scoreMap;
    }

    public void addScore(String key, Integer value){
        getScoreMap().put(key, value);
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

    public String getSummaryColorCode() {
        return summaryColorCode;
    }

    public void setSummaryColorCode(String summaryColorCode) {
        this.summaryColorCode = summaryColorCode;
    }

    public int getLeaderboardColor() {
        return leaderboardColor;
    }

    public void setLeaderboardColor(int leaderboardColor) {
        this.leaderboardColor = leaderboardColor;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    @Override
    public int compareTo(Object o) {
        return (getProviderName()).compareTo((((ProviderReportStatistic)o).getProviderName()));
    }
}
