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

import java.util.ArrayList;
import java.util.List;

public class ObsGroups {
    public final String name;
    public final List<ObsData> list;

    public ObsGroups(String name) {
        this.name = name;
        list = new ArrayList<ObsData>();
    }

    public int size() {
        return list.size();
    }

    public ObsData get(int i) {
        return list.get(i);
    }
}
