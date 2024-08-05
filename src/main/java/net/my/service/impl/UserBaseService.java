package net.my.service.impl;

import net.my.pojo.UserBase;
import net.my.service.IUserBaseService;
import org.springframework.stereotype.Service;

@Service
public class UserBaseService implements IUserBaseService {
    @Override
    public UserBase findUserByAccount(String id) {
        return UserBase.builder().userId(id).name("my-test-user").build();
    }
}
