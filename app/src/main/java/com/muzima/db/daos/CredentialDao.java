package com.muzima.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.muzima.db.entities.Credential;

import java.util.List;

@Dao
public interface CredentialDao {

    @Query("SELECT * FROM credential WHERE username=:username")
    Credential getCredentialByUsername(String username);

    @Update
    void update(Credential credential);

    @Insert
    void insert(Credential credential);

}
