package net.my.service;

import net.my.pojo.UserBase;

public interface IUserBaseService {
    UserBase findUserByAccount(String id);
}
