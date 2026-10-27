package com.zincoid.nullbot.adapter.qq;

/**
 * QQ 会话目标。
 * <p>QQ conversation target.</p>
 *
 * @param group whether the target is a group
 * @param id    the group openid or user openid
 */
public record QqTarget(boolean group, String id) {
}
