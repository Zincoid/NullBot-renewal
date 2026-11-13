package com.zincoid.nullbot.platform.util;

/**
 * 用户显示标签：把昵称与平台 ID 组装为 name(id)。
 * <p>User display label: joins a nickname and platform id as name(id).</p>
 */
public final class UserLabels {

    private UserLabels() {
    }

    /**
     * 组装用户显示标签，昵称为空时回退为 ID。
     * <p>Builds the user display label, falling back to the id when the nickname is blank.</p>
     *
     * @param name the nickname, nullable
     * @param id   the platform user id
     * @return the label, e.g. {@code Alice(10001)}
     */
    public static String of(String name, String id) {
        String value = name == null || name.isBlank() ? id : name;
        return value + "(" + id + ")";
    }
}
