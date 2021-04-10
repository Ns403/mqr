package com.molicloud.mqr.plugin.core.enums;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ns
 */

@AllArgsConstructor
public enum MessageTypeEnum {
    /**
     * 消息类型
     */
    FLASH_MSG("[闪照]"),
    FORWARD_MSG("[转发消息]"),
    ;

    private final String strMsgType;
    private static final List<String> MSG_TYPE_LIST;

    static{
        MSG_TYPE_LIST = Stream.of(MessageTypeEnum.values()).map(messageTypeEnum -> messageTypeEnum.strMsgType).collect(Collectors.toList());
    }

    public static boolean checkMsgType(String msgType) {
        return MSG_TYPE_LIST.contains(msgType);
    }


}


