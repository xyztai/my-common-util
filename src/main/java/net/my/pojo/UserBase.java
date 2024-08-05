package net.my.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserBase {
    private String userId;
    private String name;
    private String password;
}
