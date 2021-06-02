package com.muzima.model;

import com.muzima.model.enums.CardsSummaryCategory;

public class SummaryCard {

    private CardsSummaryCategory category;
    private String title;
    private long count;

    public SummaryCard() {
    }

    public SummaryCard(CardsSummaryCategory category, String title, long count) {
        this.category = category;
        this.title = title;
        this.count = count;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public CardsSummaryCategory getCategory() {
        return category;
    }

    public void setCategory(CardsSummaryCategory category) {
        this.category = category;
    }
}
