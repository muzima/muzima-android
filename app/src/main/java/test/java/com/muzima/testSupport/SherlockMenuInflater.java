/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

// Copyright 2012 Square, Inc.
package com.muzima.testSupport;

import android.content.Context;
import android.util.AttributeSet;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.SubMenu;

import org.robolectric.internal.Implementation;
import org.robolectric.res.MenuNode;
import org.robolectric.res.ResourceLoader;
import org.robolectric.tester.android.util.ResName;
import org.robolectric.util.I18nException;

import static org.robolectric.Robolectric.shadowOf;

/**
 * Inflates menus that are part of ActionBarSherlock instead. Uses ABS customViews {@link
 * com.actionbarsherlock.view.Menu} instead of the stock one.
 */
public class SherlockMenuInflater extends MenuInflater {
    private final Context context;

    public SherlockMenuInflater(Context context) {
        super(context);
        this.context = context;
    }

    @Implementation
    public void inflate(int resource, Menu root) {
        String qualifiers = shadowOf(context.getResources().getConfiguration()).getQualifiers();
        ResourceLoader resourceLoader = shadowOf(context).getResourceLoader();
        ResName resName = shadowOf(context).getResName(resource);
        MenuNode menuNode = resourceLoader.getMenuNode(resName, qualifiers);

        try {
            addChildrenInGroup(menuNode, 0, root);
        } catch (I18nException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("error inflating " + resName, e);
        }
    }

    private void addChildrenInGroup(MenuNode source, int groupId, Menu root) {
        for (MenuNode child : source.getChildren()) {
            String name = child.getName();
            AttributeSet attributes = shadowOf(context).createAttributeSet(child.getAttributes(), null);
            if (name.equals("item")) {
                if (child.isSubMenuItem()) {
                    SubMenu sub = root.addSubMenu(groupId,
                            attributes.getAttributeResourceValue("android", "id", 0),
                            0, attributes.getAttributeValue("android", "title"));
                    MenuNode subMenuNode = child.getChildren().get(0);
                    addChildrenInGroup(subMenuNode, groupId, sub);
                } else {
                    root.add(groupId,
                            attributes.getAttributeResourceValue("android", "id", 0),
                            0, attributes.getAttributeValue("android", "title"));
                }
            } else if (name.equals("group")) {
                int newGroupId = attributes.getAttributeResourceValue("android", "id", 0);
                addChildrenInGroup(child, newGroupId, root);
            }
        }
    }
}