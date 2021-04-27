package com.molicloud.mqr.plugin.mute.common.contants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    MUTE_ALL_ED("管理员已开启全体禁言"),
    UN_MUTE_ALL("解除全体禁言"),
    UN_MUTE_ALL_ED("管理员已关闭全体禁言"),
    AUTO_MUTE_ALL("开启宵禁"),
    UN_AUTO_MUTE_ALL("关闭宵禁"),
    ;
    private final String keyword;
    private static final Map<String, MuteEnum> DEFAULT_MAP;
    static {
        DEFAULT_MAP = Stream.of(MuteEnum.values()).collect(Collectors.toMap(MuteEnum::getKeyword, Function.identity()));
    }
    public static MuteEnum getMuteEnum(String keyword){
        return DEFAULT_MAP.get(keyword);
    }
}
