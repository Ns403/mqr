package com.molicloud.mqr.plugin.mute.common.contants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Ns
 */
@Getter
@AllArgsConstructor
public enum MuteEnum {
    /**
     * 关键字枚举
     */
    MUTE_ALL("全体禁言"),
    UN_MUTE_ALL("解除全体禁言"),
    AUTO_MUTE_ALL("开启宵禁"),
    UN_AUTO_MUTE_ALL("关闭宵禁"),
    ;
    private final String keyword;
}
