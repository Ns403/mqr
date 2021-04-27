package com.molicloud.mqr.framework.listener;

import com.molicloud.mqr.entity.GroupMuteAllDao;
import com.molicloud.mqr.framework.listener.event.PluginResultEvent;
import com.molicloud.mqr.mapper.GroupMuteAllConfigMapper;
import com.molicloud.mqr.plugin.core.PluginParam;
import com.molicloud.mqr.plugin.core.PluginResult;
import com.molicloud.mqr.plugin.core.enums.MessageTypeEnum;
import com.molicloud.mqr.plugin.core.enums.RobotEventEnum;
import com.molicloud.mqr.plugin.core.RobotContextHolder;
import com.molicloud.mqr.framework.util.ActionUtil;
import com.molicloud.mqr.framework.util.MessageUtil;
import com.molicloud.mqr.framework.util.PluginHookUtil;
import com.molicloud.mqr.plugin.core.message.make.Ats;
import com.molicloud.mqr.service.RobotFriendService;
import com.molicloud.mqr.service.RobotGroupMemberService;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.message.data.*;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 监听插件返回结果的处理事件
 *
 * @author feitao yyimba@qq.com
 * @since 2020/11/12 10:42 上午
 */
@Component
@Slf4j
public class PluginResultListener {

    @Autowired
    private RobotFriendService robotFriendService;

    @Autowired
    private RobotGroupMemberService robotGroupMemberService;
    @Autowired
    private GroupMuteAllConfigMapper groupMuteAllConfigMapper;

    private static final LocalTime ZERO_LOCAL_TIME = LocalTime.of(0, 0);
    private static final LocalTime SEVEN_LOCAL_TIME = LocalTime.of(7, 0);

    @Async
    @EventListener(PluginResultEvent.class)
    public void handlerResult(PluginResultEvent pluginResultEvent) {
        // 获取机器人实例
        Bot bot = Bot.getInstance(Long.parseLong(RobotContextHolder.getRobot().getQq()));
        // 插件入参
        PluginParam pluginParam = pluginResultEvent.getPluginParam();
        //处理
        log.info("sendMsg:{}",pluginParam.getData());
        if (MessageTypeEnum.checkMsgType(String.valueOf(pluginParam.getData()))) {
            MessageChain message = (MessageChain) pluginParam.getMessage();
            MessageSource.recall(message);
            return;
        }
        // 机器人事件枚举
        RobotEventEnum robotEventEnum = pluginParam.getRobotEventEnum();
        // 插件返回的结果
        PluginResult pluginResult = pluginResultEvent.getPluginResult();
        // 判断是否为消息类型的事件
        if (robotEventEnum.isMessageEvent()) {
            switch (robotEventEnum) {
                case GROUP_MSG:
                    handlerGroupMessage(bot, pluginParam, pluginResult, pluginResultEvent.getPluginHookName());
                    break;
                case FRIEND_MSG:
                    handlerFriendMessage(bot, pluginParam, pluginResult, pluginResultEvent.getPluginHookName());
                    break;
                case TEMP_MSG:
                    handlerTempMessage(bot, pluginParam, pluginResult, pluginResultEvent.getPluginHookName());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 处理群消息
     *
     * @param bot
     * @param pluginParam
     * @param pluginResult
     * @param hookName
     */
    private void handlerGroupMessage(Bot bot, PluginParam pluginParam, PluginResult pluginResult, String hookName) {
        Group group = bot.getGroup(Long.parseLong(pluginParam.getTo()));
        if (Objects.isNull(group)){
            log.info("组信息为空");
            return;
        }
        //机器人0点到七点自动撤回所有消息
        LocalTime now = LocalTime.now();
        String to = pluginParam.getTo();
        GroupMuteAllDao groupMuteAllDao = groupMuteAllConfigMapper.selectByGroupId(to);
        if (Objects.isNull(groupMuteAllDao)) {
            groupMuteAllDao = GroupMuteAllDao.builder().groupId(String.valueOf(group.getId())).muteAllStatus(0).autoMuteAllStatus(0).build();
            groupMuteAllConfigMapper.insertRecord(groupMuteAllDao);
        }
        if (!pluginResult.isMuteAllPlugin()) {
            //判断是否开启全体禁言
            if (groupMuteAllDao.getMuteAllStatus() == 1) {
                muteAllMsg(pluginParam, group);
                return;
            }
            if (SEVEN_LOCAL_TIME.isAfter(now) && ZERO_LOCAL_TIME.isBefore(now) && groupMuteAllDao.getAutoMuteAllStatus() == 1) {
                curfew(pluginParam, group, now);
                return;
            }
        }
        if (pluginResult.getMessage() != null) {
            Message groupMessage = MessageUtil.convertGroupMessage(pluginResult.getMessage(), group);
            if (groupMessage != null) {
                group.sendMessage(groupMessage);
            }
        }
        if (pluginResult.getAction() != null) {
            ActionUtil.handlerGroupAction(group, pluginResult.getAction(), pluginParam);
        }
        // 持有/释放插件钩子
        if (PluginHookUtil.actionGroupMemberPluginHook(pluginParam.getTo(), pluginParam.getFrom(), hookName, pluginParam.getData().toString(), pluginResult.getHold())) {
            robotGroupMemberService.handlerHoldAction(pluginParam.getTo(), pluginParam.getFrom(), pluginResult.getHold(), hookName, pluginParam.getData().toString());
        }
    }

    /**
     * 处理群禁言后发消息
     * @param pluginParam
     * @param group
     */
    private void muteAllMsg(PluginParam pluginParam, Group group) {
        try {
            NormalMember normalMember = group.getMembers().get(Long.parseLong(pluginParam.getFrom()));
            if (Objects.nonNull(normalMember)) {
                Ats ats = new Ats();
                ats.setMids(Lists.newArrayList(pluginParam.getFrom()));
                ats.setContent("现在是全体禁言，您不能发言！！再次尝试将禁言一个月！！");
                Message message = MessageUtil.convertGroupMessage(ats, group);
                if (Objects.nonNull(message)) {
                    group.sendMessage(message);
                }
            }
            MessageChain message = (MessageChain) pluginParam.getMessage();
            MessageSource.recall(message);
        } catch (Exception e) {
            log.error("可能是管理员发言无权限执行", e);
        }
    }

    /**
     * 处理群禁言后发消息
     * @param pluginParam
     * @param group
     * @param now
     */
    private void curfew(PluginParam pluginParam, Group group, LocalTime now) {
        try {
            ContactList<NormalMember> members = group.getMembers();
            NormalMember normalMember = members.get(Long.parseLong(pluginParam.getFrom()));
            Duration between = Duration.between(now, SEVEN_LOCAL_TIME);
            if (Objects.nonNull(normalMember)) {
                Ats ats = new Ats();
                ats.setMids(Lists.newArrayList(pluginParam.getFrom()));
                ats.setContent("不好意思，由于现在是宵禁时间；加上赛雷Robot全体禁言有bug，不得已配置自动撤回及禁言至宵禁结束，敬请谅解！");
                Message message = MessageUtil.convertGroupMessage(ats, group);
                if (Objects.nonNull(message)) {
                    group.sendMessage(message);
                }
                normalMember.mute((int) between.getSeconds());
            }
            MessageChain message = (MessageChain) pluginParam.getMessage();
            MessageSource.recall(message);
        } catch (Exception e) {
            log.error("可能是管理员发言无权限执行", e);
        }
    }

    /**
     * 处理好友消息
     *
     * @param bot
     * @param pluginParam
     * @param pluginResult
     * @param hookName
     */
    private void handlerFriendMessage(Bot bot, PluginParam pluginParam, PluginResult pluginResult, String hookName) {
        Friend friend = bot.getFriend(Long.parseLong(pluginParam.getFrom()));
        if (pluginResult.getMessage() != null) {
            Message friendMessage = MessageUtil.convertPersonalMessage(pluginResult.getMessage(), friend);
            if (friendMessage != null) {
                friend.sendMessage(friendMessage);
            }
        }
        if (pluginResult.getAction() != null) {
            ActionUtil.handlerFriendAction(friend, pluginResult.getAction());
        }
        // 持有/释放插件钩子
        if (PluginHookUtil.actionFriendPluginHook(pluginParam.getFrom(), hookName, pluginParam.getData().toString(), pluginResult.getHold())) {
            robotFriendService.handlerHoldAction(pluginParam.getFrom(), pluginResult.getHold(), hookName, pluginParam.getData().toString());
        }
    }

    /**
     * 处理临时消息
     *
     * @param bot
     * @param pluginParam
     * @param pluginResult
     * @param hookName
     */
    private void handlerTempMessage(Bot bot, PluginParam pluginParam, PluginResult pluginResult, String hookName) {
        Member member = bot.getGroup(Long.parseLong(pluginParam.getTo())).get(Long.parseLong(pluginParam.getFrom()));
        if (pluginResult.getMessage() != null) {
            Message groupMessage = MessageUtil.convertPersonalMessage(pluginResult.getMessage(), member);
            if (groupMessage != null) {
                member.sendMessage(groupMessage);
            }
        }
        if (pluginResult.getAction() != null) {
            ActionUtil.handlerMemberAction(member, pluginResult.getAction());
        }
        // 持有/释放插件钩子
        if (PluginHookUtil.actionFriendPluginHook(pluginParam.getFrom(), hookName, pluginParam.getData().toString(), pluginResult.getHold())) {
            robotFriendService.handlerHoldAction(pluginParam.getFrom(), pluginResult.getHold(), hookName, pluginParam.getData().toString());
        }
    }
}