package com.molicloud.mqr.framework.util;

import cn.hutool.core.collection.CollUtil;
import com.molicloud.mqr.plugin.core.PluginParam;
import com.molicloud.mqr.plugin.core.action.Action;
import com.molicloud.mqr.plugin.core.action.KickAction;
import com.molicloud.mqr.plugin.core.action.MuteAction;
import com.molicloud.mqr.plugin.core.action.UnmuteAction;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件动作工具类
 *
 * @author feitao yyimba@qq.com
 * @since 2020/11/17 11:38 上午
 */
@Slf4j
@UtilityClass
public class ActionUtil {

    /**
     * 处理群动作
     *  @param group
     * @param action
     * @param pluginParam
     */
    public void handlerGroupAction(Group group, Action action, PluginParam pluginParam) {
        if (action != null) {
            if (action.getIsMuteAll() != null) {
                group.getSettings().setMuteAll(action.getIsMuteAll());
                return;
            }
            List<String> ids = action.getIds();
            ContactList<NormalMember> memberContactList = group.getMembers();
            List<NormalMember> memberList = ids.stream().map(mid -> memberContactList.get(Long.parseLong(mid))).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(memberList)) {
                if (action instanceof MuteAction) {
                    MuteAction muteAction = (MuteAction) action;
                    memberList.stream().forEach(member -> member.mute(muteAction.getSeconds()));
                } else if (action instanceof UnmuteAction) {
                    memberList.stream().forEach(member -> member.unmute());
                } else if (action instanceof KickAction) {
                    //踢人逻辑处理
                    if (pluginParam != null && pluginParam.getMessage() instanceof Message) {
                        MessageChain message = (MessageChain) pluginParam.getMessage();
                        MessageSource.recall(message);
                    }
                    memberList.stream().forEach(member -> member.kick(""));
                }
            }
        }
    }

    /**
     * 处理好友动作
     *
     * @param friend
     * @param action
     */
    public void handlerFriendAction(Friend friend, Action action) {
        if (action != null) {
            log.debug("好友消息不支持此动作");
        }
    }

    /**
     * 处理临时好友动作
     *
     * @param member
     * @param action
     */
    public void handlerMemberAction(Member member, Action action) {
        if (action != null) {
            log.debug("临时好友消息不支持此动作");
        }
    }
}