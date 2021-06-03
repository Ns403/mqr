package com.molicloud.mqr.plugin.manager;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.molicloud.mqr.plugin.core.AbstractPluginExecutor;
import com.molicloud.mqr.plugin.core.PluginInfo;
import com.molicloud.mqr.plugin.core.PluginParam;
import com.molicloud.mqr.plugin.core.PluginResult;
import com.molicloud.mqr.plugin.core.action.Action;
import com.molicloud.mqr.plugin.core.action.impl.BlackKickAction;
import com.molicloud.mqr.plugin.core.action.impl.KickAction;
import com.molicloud.mqr.plugin.core.action.impl.MuteAction;
import com.molicloud.mqr.plugin.core.action.impl.UnmuteAction;
import com.molicloud.mqr.plugin.core.annotation.PHook;
import com.molicloud.mqr.plugin.core.define.AtDef;
import com.molicloud.mqr.plugin.core.define.FaceDef;
import com.molicloud.mqr.plugin.core.define.RobotDef;
import com.molicloud.mqr.plugin.core.enums.ChoiceEnum;
import com.molicloud.mqr.plugin.core.enums.ExecuteTriggerEnum;
import com.molicloud.mqr.plugin.core.enums.RobotEventEnum;
import com.molicloud.mqr.plugin.core.event.MessageEvent;
import com.molicloud.mqr.plugin.core.message.MessageBuild;
import com.molicloud.mqr.plugin.core.message.make.Ats;
import com.molicloud.mqr.plugin.core.message.make.Expression;
import com.molicloud.mqr.service.BlackUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 机器人管理插件 TODO 待优化
 *
 * @author wispx wisp-x@qq.com
 * @since 2020/11/28 9:11 上午
 */
@Slf4j
@Component
public class ManagerPluginExecutor extends AbstractPluginExecutor {

    /**
     * 被操作对象QQ列表
     */
    private List<String> ids = new LinkedList<>();
    @Autowired
    private BlackUserService blackUserService;

    /**
     * 指令列表
     */
    private final String[] commands = {"禁言", "解禁", "踢人", "拉黑"};

    @PHook(name = "Manager",
            listeningAllMessage = true,
            startsKeywords = {"禁言", "解禁", "踢人", "拉黑"},
            equalsKeywords = {"开启自动入群", "关闭自动入群", "开启入群欢迎", "关闭入群欢迎", "设置入群欢迎语"},
            robotEvents = {RobotEventEnum.GROUP_MSG, RobotEventEnum.MEMBER_JOIN, RobotEventEnum.MEMBER_JOIN_REQUEST})
    public PluginResult messageHandler(PluginParam pluginParam) {
        if (RobotEventEnum.GROUP_MSG.equals(pluginParam.getRobotEventEnum())) {
            return handlerYihao(pluginParam);
        } else if (RobotEventEnum.MEMBER_JOIN.equals(pluginParam.getRobotEventEnum())) {
            return handlerMemberJoinEvent(pluginParam);
        } else if (RobotEventEnum.MEMBER_JOIN_REQUEST.equals(pluginParam.getRobotEventEnum())) {
            return handlerMemberJoinRequestEvent(pluginParam);
        }
        return PluginResult.noReply();
    }

    /**
     * 禁言/解禁/踢人操作
     *
     * @return
     */
    private PluginResult handlerYihao(PluginParam pluginParam) {
        if (!ExecuteTriggerEnum.HOLD.equals(pluginParam.getExecuteTriggerEnum()) &&
                !ExecuteTriggerEnum.KEYWORD.equals(pluginParam.getExecuteTriggerEnum())) {
            return PluginResult.noReply();
        }

        // 实例化插件回复结果
        PluginResult pluginResult = new PluginResult();
        pluginResult.setProcessed(true);
        // 判断消息发送者是否为管理员
        if (!Arrays.asList(getAdmins()).contains(pluginParam.getFrom())) {
            MessageBuild messageBuild = new MessageBuild();
            Ats ats = new Ats();
            ats.setMids(Arrays.asList(pluginParam.getFrom()));
            ats.setContent("您没有权限执行该操作");
            messageBuild.append(ats);
            messageBuild.append(new Expression(FaceDef.zuohengheng));
            pluginResult.setMessage(messageBuild);
            return pluginResult;
        }

        String message = String.valueOf(pluginParam.getData());
        ManagerSetting managerSetting = getHookSetting(ManagerSetting.class);
        if (managerSetting == null) {
            managerSetting = new ManagerSetting();
        }

        // 判断是否通过主动持有进入
        if (ExecuteTriggerEnum.HOLD.equals(pluginParam.getExecuteTriggerEnum())) {
            managerSetting.setWelcomeMessage(pluginParam.getData().toString());
            saveHookSetting(managerSetting);
            pluginResult.setMessage("入群欢迎语设置成功");
            return pluginResult;
        }

        switch (message) {
            case "开启自动入群":
                managerSetting.setAutoJoin(true);
                saveHookSetting(managerSetting);
                pluginResult.setMessage("自动同意加群已开启");
                return pluginResult;
            case "关闭自动入群":
                managerSetting.setAutoJoin(false);
                saveHookSetting(managerSetting);
                pluginResult.setMessage("自动同意加群已关闭");
                return pluginResult;
            case "开启入群欢迎":
                managerSetting.setAutoWelcomeMessage(true);
                saveHookSetting(managerSetting);
                pluginResult.setMessage("入群欢迎消息已开启");
                return pluginResult;
            case "关闭入群欢迎":
                managerSetting.setAutoWelcomeMessage(false);
                saveHookSetting(managerSetting);
                pluginResult.setMessage("入群欢迎消息已关闭");
                return pluginResult;
            case "设置入群欢迎语":
                Ats ats = new Ats();
                ats.setMids(Arrays.asList(pluginParam.getFrom()));
                ats.setContent("请在下条消息告诉我入群欢迎语");
                pluginResult.setHold(true);
                pluginResult.setMessage(ats);
                return pluginResult;
            default:
                break;
        }

        String command = getCommand(message);
        List<AtDef> atDefs = pluginParam.getAts();
        ids = atDefs.stream().map(AtDef::getId).distinct().collect(Collectors.toList());
        boolean muteAll = !("全体禁言".equals(command) || "解除全体禁言".equals(command));
        if (ids.isEmpty()&&muteAll ) {
            pluginResult.setMessage("未选择操作对象");
            return pluginResult;
        }
        pluginResult.setMessage("已执行！");
        switch (command) {
            case "禁言":
                return mute(pluginResult, getArgsContent(atDefs, message));
            case "解禁":
                pluginResult.setAction(new UnmuteAction(ids));
                break;
            case "全体禁言":
                pluginResult.setAction(Action.builder().isMuteAll(true).build());
                break;
            case "解除全体禁言":
                pluginResult.setAction(Action.builder().isMuteAll(false).build());
                break;
            case "拉黑":
                //保存拉黑记录
                blackUserService.addBlackUserList(ids);
                BlackKickAction blackKickAction = new BlackKickAction();
                blackKickAction.setIds(ids);
                pluginResult.setAction(blackKickAction);
                break;
            case "踢人":
                pluginResult.setAction(new KickAction(ids));
                break;
            default:
                pluginResult.setMessage("未执行任何操作");
        }
        return pluginResult;
    }

    /**
     * 处理申请入群事件
     *
     * @param pluginParam
     * @return
     */
    private PluginResult handlerMemberJoinRequestEvent(PluginParam pluginParam) {
        Boolean isBlack = blackUserService.checkUserInBlack(pluginParam.getFrom());
        if (isBlack) {
            return PluginResult.reply(ChoiceEnum.REJECT_IN_BLACK);
        }
        // 获取管理配置
        ManagerSetting managerSetting = getHookSetting(ManagerSetting.class);
        // 如果配置不为空，且没有开启自动加群，则忽略此申请
        if (managerSetting == null || !managerSetting.getAutoJoin()) {
            return PluginResult.noReply();
        }
        // 判断是否开启了自动欢迎信息
        if (managerSetting.getAutoWelcomeMessage()) {
            // 推送欢迎进入的消息
            pushWelcomeJoinMessage(pluginParam, managerSetting.getWelcomeMessage());
        }
        // 同意入群申请
        return PluginResult.reply(ChoiceEnum.ACCEPT);
    }

    /**
     * 处理成员已经加群事件
     *
     * @param pluginParam
     * @return
     */
    private PluginResult handlerMemberJoinEvent(PluginParam pluginParam) {
        // 获取管理配置
        ManagerSetting managerSetting = getHookSetting(ManagerSetting.class);
        if (managerSetting != null && managerSetting.getAutoWelcomeMessage()) {
            // 推送欢迎进入的消息
            pushWelcomeJoinMessage(pluginParam, managerSetting.getWelcomeMessage());
        }
        return PluginResult.reply(null);
    }

    /**
     * 推送欢迎进入的消息
     *
     * @param pluginParam
     * @param welcomeMessage
     */
    private void pushWelcomeJoinMessage(PluginParam pluginParam, String welcomeMessage) {
        // 发送At消息
        Ats ats = new Ats();
        ats.setMids(Arrays.asList(pluginParam.getFrom()));
        ats.setContent(welcomeMessage);

        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setRobotEventEnum(RobotEventEnum.GROUP_MSG);
        messageEvent.setToIds(Arrays.asList(pluginParam.getTo()));
        messageEvent.setMessage(ats);
        // 异步推送群消息
        pushMessage(messageEvent);
    }

    /**
     * 禁言用户
     *
     * @param pluginResult
     * @param args
     * @return
     */
    private PluginResult mute(PluginResult pluginResult, String args) {
        if (args.isEmpty()) {
            pluginResult.setMessage("指令错误");
            return pluginResult;
        }
        Integer seconds;
        String arg = args.replaceAll("[^\\u4e00-\\u9fa5]", "");
        String[] argsArr = args.split(" ");
        if (argsArr.length==1) {
            pluginResult.setMessage("禁言格式错误，请按照'禁言 @群友 时间'设置");
            return pluginResult;
        }
        String days = argsArr[argsArr.length - 1];
        String value = getArgNum(days);
        if (value.isEmpty() || !isInteger(value)) {
            pluginResult.setMessage("禁言时间错误");
            return pluginResult;
        }
        Integer val = Integer.valueOf(value);
        switch (arg) {
            case "分钟":
                seconds = val * 60;
                break;
            case "小时":
                seconds = val * 3600;
                break;
            case "天":
                seconds = val * 86400;
                break;
            case "月":
            case "个月":
                seconds = 86400 * 30;
                break;
            default:
                pluginResult.setMessage("指令错误");
                return pluginResult;
        }

        if (seconds > 2592000) {
            pluginResult.setMessage("禁言最长时间为 30 天");
            return pluginResult;
        }

        pluginResult.setAction(new MuteAction(ids, seconds));
        pluginResult.setMessage("操作成功");
        return pluginResult;
    }

    /**
     * 获取指令内容
     *
     * @param atDefs
     * @param message
     * @return
     */
    private String getArgsContent(List<AtDef> atDefs, String message) {
        String content = message.replace(getCommand(message), "").trim(); // 指令后面的内容
        for (AtDef atDef : atDefs) content = content.replaceAll(atDef.getNick(), "");
        return content.trim();
    }

    /**
     * 获取消息指令
     *
     * @param message
     * @return
     */
    private String getCommand(String message) {
        for (String command : commands) {
            if (command != null && StrUtil.startWith(message, command)) {
                return command;
            }
        }
        return "";
    }

    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private static String getArgNum(String str) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    @Override
    public PluginInfo getPluginInfo() {
        PluginInfo pluginInfo = new PluginInfo();
        Map<Integer, String> build = MapUtil.builder(10001, "create unique index u_idx_black_user_qq on robot_plugin_black_user (qq);").build();
        pluginInfo.setAuthor("NS");
        pluginInfo.setName("Manager");
        pluginInfo.setVersion(10001);
        pluginInfo.setInitScript("create table robot_plugin_black_user\n" +
                "(\n" +
                "    id INTEGER not null\n" +
                "        constraint robot_plugin_black_user_pk\n" +
                "            primary key autoincrement,\n" +
                "    qq varchar(32) not null,\n" +
                "    status tinyint default 1 not null,\n" +
                "    create_time timestamp default current_timestamp not null,\n" +
                "    update_time timestamp\n" +
                ");");
        pluginInfo.setUpdateScriptList(build);
        return pluginInfo;
    }
}
