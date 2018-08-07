/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.graphics.Color;

import java.util.Random;

public enum CustomColor {
    ELLE_BELLE("#7FAF1B"),
    DUTCH_TEAL("#1693A5"),
    GRUMMPY("#CC527A"),
    YUM("#098786"),
    SOMEWHAT_PURPLE("#C19652"),
    ROSEWOOD("#901F0F"),
    CLOCKWORK_ORANGE("#E73525"),
    LOST_SOMEWHERE("#48A09B"),
    CHRISTMAS_BLUE("#2A8FBD"),
    STRAWBERRY_SAUCE("#F20F62"),
    DARTH_GREY("#666666"),
    CENTENARY_BLUE("#7BA5D1"),
    DINK_PINK("#FF0066"),
    MAD_RED("#F02311"),
    RESPBERRY_SAUCE("#AB0743"),
    ALLERGIC_RED("#FF4040"),
    HUMAN_DRESS("#57553c"),
    HIGH_SKYBLUE("#107FC9"),
    ROUNGE("#FF6600"),
    EYES_IN_SKY("#7D96FF"),
    BLESSING("#DB1750"),
    ACQUA("#036564"),
    GRUBBY("#308000"),
    POOLSIDE("#34BEDA"),
    CREEP("#0B8C8F"),
    BUZZ("#6991AA"),
    BLUSH("#E05D6F"),
    COOL_AID("#A40778");

    private final int color;
    private static final Random random = new Random();

    public int getColor() {
        return color;
    }

    CustomColor(String color){
        this.color = Color.parseColor(color);
    }

    public static int getRandomColor(){
        int numOfColors = CustomColor.values().length;
        int colorPos = random.nextInt(numOfColors);
        return CustomColor.values()[colorPos].color;
    }

    public static int getOrderedColor(int position){
        int pos = position % values().length;
        return CustomColor.values()[pos].color;
    }
}
