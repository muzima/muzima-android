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
