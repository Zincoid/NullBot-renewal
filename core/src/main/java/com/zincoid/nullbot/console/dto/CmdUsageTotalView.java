package com.zincoid.nullbot.console.dto;

/**
 * 控制台全量指令用量视图。
 * <p>Console view of the all-time command usage.</p>
 *
 * @param total the all-time command call count
 */
public record CmdUsageTotalView(long total) {
}
