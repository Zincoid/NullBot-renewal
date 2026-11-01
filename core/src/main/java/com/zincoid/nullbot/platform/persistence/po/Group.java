package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("`group`")
public class Group {

    private String id;
    private String name;
    private Access access;
    private Boolean cmdEnabled = true;
    private Boolean aiChatEnabled = true;

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
        this.access = Access.ADMIN;
        this.cmdEnabled = true;
        this.aiChatEnabled = true;
    }
}
