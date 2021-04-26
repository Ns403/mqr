package com.molicloud.mqr.framework.handler;

import cn.hutool.core.collection.CollUtil;
import com.molicloud.mqr.common.define.RobotAllowList;
import com.molicloud.mqr.common.enums.SettingEnum;
import com.molicloud.mqr.framework.listener.event.PluginResultEvent;
import com.molicloud.mqr.plugin.core.PluginParam;
import com.molicloud.mqr.plugin.core.define.AtDef;
import com.molicloud.mqr.plugin.core.enums.ChoiceEnum;
import com.molicloud.mqr.plugin.core.enums.MemberJoinEnum;
import com.molicloud.mqr.plugin.core.enums.RobotEventEnum;
import com.molicloud.mqr.framework.util.PluginUtil;
import com.molicloud.mqr.service.SysSettingService;
import kotlin.coroutines.CoroutineContext;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 事件监听处理器
 *
 * @author feitao yyimba@qq.com
 * @since 2020/11/4 5:01 下午
 */
@Slf4j
@Component
public class EventListeningHandler extends SimpleListenerHost {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SysSettingService sysSettingService;

    /**
     * 监听群消息
     *
     * @param event
     * @return
     */
    @EventHandler
    public ListeningStatus onGroupMessage(GroupMessageEvent event) {
        // 群白名单过滤
        if (groupAllowListFilter(String.valueOf(event.getGroup().getId()))) {
            // 实例化插件入参对象
            PluginParam pluginParam = new PluginParam();
            pluginParam.setFrom(String.valueOf(event.getSender().getId()));
            pluginParam.setTo(String.valueOf(event.getGroup().getId()));
            pluginParam.setData(event.getMessage().contentToString());
            pluginParam.setMessage(event.getMessage());
            pluginParam.setRobotEventEnum(RobotEventEnum.GROUP_MSG);
            // 获取消息中的At信息
            List<AtDef> atDefs = new LinkedList<>();
            boolean isAt = getAtInfo(event.getGroup(), event.getMessage(), String.valueOf(event.getBot().getId()), atDefs);
            pluginParam.setAt(isAt);
            pluginParam.setAts(atDefs);
            // 处理消息事件
            handlerMessageEvent(pluginParam);
        }
        // 保持监听
        return ListeningStatus.LISTENING;
    }

    /**
     * 监听好友消息
     *
     * @param event
     * @return
     */
    @EventHandler
    public ListeningStatus onFriendsMessage(FriendMessageEvent event) {
        // 好友白名单过滤
        if (friendAllowListFilter(String.valueOf(event.getFriend().getId()))) {
            // 实例化插件入参对象
            PluginParam pluginParam = new PluginParam();
            pluginParam.setFrom(String.valueOf(event.getFriend().getId()));
            pluginParam.setTo(String.valueOf(event.getBot().getId()));
            pluginParam.setData(event.getMessage().contentToString());
            pluginParam.setRobotEventEnum(RobotEventEnum.FRIEND_MSG);
            // 处理消息事件
            handlerMessageEvent(pluginParam);
        }
        // 保持监听
        return ListeningStatus.LISTENING;
    }

    /**
     * 监听群成员的临时消息
     *
     * @param event
     * @return
     */
    @EventHandler
    public ListeningStatus onTempMessage(GroupTempMessageEvent event) {
        // 好友白名单过滤
        if (friendAllowListFilter(String.valueOf(event.getSender().getId()))) {
            // 实例化插件入参对象
            PluginParam pluginParam = new PluginParam();
            pluginParam.setFrom(String.valueOf(event.getSender().getId()));
            pluginParam.setTo(String.valueOf(event.getGroup().getId()));
            pluginParam.setData(event.getMessage().contentToString());
            pluginParam.setRobotEventEnum(RobotEventEnum.TEMP_MSG);
            // 处理消息事件
            handlerMessageEvent(pluginParam);
        }
        // 保持监听
        return ListeningStatus.LISTENING;
    }

    /**
     * 监听入群申请事件
     *
     * 注意：如果是机器人同意的入群申请，则机器人无法监听群成员已经加群的事件
     *
     * @param event
     * @return
     */
    @EventHandler
    public ListeningStatus onMemberJoinRequest(MemberJoinRequestEvent event) {
        // 群白名单过滤
        if (groupAllowListFilter(String.valueOf(event.getGroup().getId()))) {
            // 实例化插件入参对象
            PluginParam pluginParam = new PluginParam();
            pluginParam.setFrom(String.valueOf(event.getFromId()));
            pluginParam.setTo(String.valueOf(event.getGroup().getId()));
            pluginParam.setData(event.getMessage());
            pluginParam.setRobotEventEnum(RobotEventEnum.MEMBER_JOIN_REQUEST);
            // 处理入群申请事件
            handlerMemberJoinRequestEvent(pluginParam, event);
        }
        // 保持监听
        return ListeningStatus.LISTENING;
    }

    @EventHandler
    public ListeningStatus muteAllEvent(GroupMuteAllEvent event) {
        if (event.getNew()) {
            //执行记录禁言群号
        } else {
            //执行解除禁言群号
            // 处理消息事件
        }
        return ListeningStatus.LISTENING;
    }

    /**
     * 监听群成员已经加群的事件
     *
     * 注意：如果是机器人同意的入群申请，则机器人无法监听群成员已经加群的事件
     *
     * @param event
     * @return
     */
    @EventHandler
    public ListeningStatus onMemberJoin(MemberJoinEvent event) {
        // 群白名单过滤
        if (groupAllowListFilter(String.valueOf(event.getGroup().getId()))) {
            // 实例化插件入参对象
            PluginParam pluginParam = new PluginParam();
            pluginParam.setFrom(String.valueOf(event.getMember().getId()));
            pluginParam.setTo(String.valueOf(event.getGroup().getId()));
            // 入群方式
            MemberJoinEnum memberJoinEnum = null;
            if (event instanceof MemberJoinEvent.Invite) {
                memberJoinEnum = MemberJoinEnum.INVITE;
            } else if (event instanceof MemberJoinEvent.Active) {
                memberJoinEnum = MemberJoinEnum.ACTIVE;
            } else if (event instanceof MemberJoinEvent.Retrieve) {
                memberJoinEnum = MemberJoinEnum.RETRIEVE;
            }
            pluginParam.setData(memberJoinEnum);
            pluginParam.setRobotEventEnum(RobotEventEnum.MEMBER_JOIN);
            // 处理已经加群事件
            handlerMemberJoinEvent(pluginParam);
        }
        // 保持监听
        return ListeningStatus.LISTENING;
    }

    @Override
    public void handleException(CoroutineContext context, Throwable exception) {
        throw new RuntimeException("在事件处理中发生异常", exception);
    }

    /**
     * 处理消息事件
     *
     * @param pluginParam
     */
    private void handlerMessageEvent(PluginParam pluginParam) {
        // 封装插件结果处理事件
        PluginResultEvent pluginResultEvent = new PluginResultEvent();
        pluginResultEvent.setPluginParam(pluginParam);
        // 执行插件，执行成功则推送异步处理的事件
        if (PluginUtil.executePlugin(pluginResultEvent)) {
            eventPublisher.publishEvent(pluginResultEvent);
        }
    }

    /**
     * 处理申请入群事件
     *
     * @param pluginParam
     * @param memberJoinRequestEvent
     */
    private void handlerMemberJoinRequestEvent(PluginParam pluginParam, MemberJoinRequestEvent memberJoinRequestEvent) {
        // 封装插件结果处理事件
        PluginResultEvent pluginResultEvent = new PluginResultEvent();
        pluginResultEvent.setPluginParam(pluginParam);
        // 执行插件，执行成功则推送异步处理的事件
        if (PluginUtil.executePlugin(pluginResultEvent)) {
            Object handler = pluginResultEvent.getPluginResult().getMessage();
            if (handler != null && handler instanceof ChoiceEnum) {
                ChoiceEnum choice = (ChoiceEnum) handler;
                if (choice.equals(ChoiceEnum.ACCEPT)) {
                    memberJoinRequestEvent.accept();
                } else if (choice.equals(ChoiceEnum.REJECT)) {
                    memberJoinRequestEvent.reject(ChoiceEnum.REJECT.getBlacklist(), ChoiceEnum.REJECT.getMessage());
                } else if (choice.equals(ChoiceEnum.IGNORE)) {
                    memberJoinRequestEvent.ignore(ChoiceEnum.IGNORE.getBlacklist());
                }
            }
        }
    }

    /**
     * 处理已经加群事件
     *
     * @param pluginParam
     */
    private void handlerMemberJoinEvent(PluginParam pluginParam) {
        // 封装插件结果处理事件
        PluginResultEvent pluginResultEvent = new PluginResultEvent();
        pluginResultEvent.setPluginParam(pluginParam);
        // 执行插件
        PluginUtil.executePlugin(pluginResultEvent);
    }

    /**
     * 获取消息中的所有At信息，并返回机器人是否被At
     *
     * @param group
     * @param messageChain
     * @param rid
     * @param atDefs
     * @return
     */
    private boolean getAtInfo(Group group, MessageChain messageChain, String rid, List<AtDef> atDefs) {
        AtomicBoolean result = new AtomicBoolean(false);
        messageChain.forEach(message -> {
            if (message instanceof At) {
                At at = (At) message;
                if (rid.equals(String.valueOf(at.getTarget()))) {
                    result.set(true);
                }
                Member member = group.get(at.getTarget());
                AtDef atDef = new AtDef();
                atDef.setId(String.valueOf(at.getTarget()));
                atDef.setNick(member.getNick());
                atDef.setNameCard(member.getNameCard());
                atDef.setRemark(member.getRemark());
                atDefs.add(atDef);
            }
        });
        return result.get();
    }

    /**
     * 群白名单过滤
     *
     * @param groupId
     * @return
     */
    private boolean groupAllowListFilter(String groupId) {
        RobotAllowList robotAllowList = sysSettingService.getSysSettingByName(SettingEnum.ROBOT_ALLOW_LIST, RobotAllowList.class);
        if (robotAllowList != null && robotAllowList.getGroupAllowListSwitch()) {
            return CollUtil.isNotEmpty(robotAllowList.getGroupAllowList()) && robotAllowList.getGroupAllowList().contains(groupId);
        }
        return true;
    }

    /**
     * 好友白名单过滤
     *
     * @param friendId
     * @return
     */
    private boolean friendAllowListFilter(String friendId) {
        RobotAllowList robotAllowList = sysSettingService.getSysSettingByName(SettingEnum.ROBOT_ALLOW_LIST, RobotAllowList.class);
        if (robotAllowList != null && robotAllowList.getFriendAllowListSwitch()) {
            return CollUtil.isNotEmpty(robotAllowList.getFriendAllowList()) && robotAllowList.getFriendAllowList().contains(friendId);
        }
        return true;
    }
}