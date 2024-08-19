package com.muzima.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.muzima.db.entities.User;

@Dao
public interface UserDao {
    @Query("select * from user where username=:username")
    User getUserByUsername(String username);

    @Insert
    void insert(User user);
}
