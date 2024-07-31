package com.muzima.utils;

import java.util.ArrayList;

public class Utils {

    public static boolean listHasElements(ArrayList<?> list){
        return list != null && !list.isEmpty() && list.size() > 0;
    }
}
