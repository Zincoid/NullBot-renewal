package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("`user`")
public class User {

    private String id;
    private String name;
    private Access access;
    private Boolean privateCmdEnabled = false;
    private Boolean privateAiChatEnabled = false;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
        this.access = Access.NORMAL;
        this.privateCmdEnabled = false;
        this.privateAiChatEnabled = false;
    }
}
