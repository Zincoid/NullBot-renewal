package com.zincoid.nullbot.platform.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("`user`")
public class User {

    private String id;
    private String name;
    private Access access;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
        this.access = Access.NORMAL;
    }

    @Override
    public String toString() {
        return String.format(
                """
                        ◆%s(%s)
                        [限权] %d 级""",
                name, id, access.getValue()
        );
    }
}
