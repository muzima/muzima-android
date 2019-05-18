package com.muzima.messaging.sqlite.database.documents;

import java.util.List;

public interface Document<T> {
    public int size();
    public List<T> getList();
}
