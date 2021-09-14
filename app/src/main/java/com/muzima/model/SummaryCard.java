/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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
