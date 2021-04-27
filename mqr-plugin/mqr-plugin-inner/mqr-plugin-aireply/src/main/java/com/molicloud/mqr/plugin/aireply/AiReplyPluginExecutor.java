package com.molicloud.mqr.plugin.aireply;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.molicloud.mqr.plugin.core.AbstractPluginExecutor;
import com.molicloud.mqr.plugin.core.PluginParam;
import com.molicloud.mqr.plugin.core.PluginResult;
import com.molicloud.mqr.plugin.core.RobotContextHolder;
import com.molicloud.mqr.plugin.core.action.Action;
import com.molicloud.mqr.plugin.core.action.MuteAndRecallAction;
import com.molicloud.mqr.plugin.core.annotation.PHook;
import com.molicloud.mqr.plugin.core.annotation.PJob;
import com.molicloud.mqr.plugin.core.define.RobotDef;
import com.molicloud.mqr.plugin.core.enums.ExecuteTriggerEnum;
import com.molicloud.mqr.plugin.core.enums.RobotEventEnum;
import com.molicloud.mqr.plugin.core.event.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 智能回复插件
 *
 * @author feitao yyimba@qq.com
 * @since 2020/11/6 3:45 下午
 */
@Slf4j
@Component
public class AiReplyPluginExecutor extends AbstractPluginExecutor {

    @Autowired
    private RestTemplate restTemplate;

    // 茉莉机器人API，以下api仅供测试，如需自定义词库和机器人名字等，请前往官网获取，获取地址 http://www.itpk.cn
    private static final String apiKey = "b7824e882d9a990e48a0e0a34d53c464";
    private static final String apiSecret = "1kn3idlxfiht";

    @PHook(name = "AiReply",
            equalsKeywords = { "设置聊天前缀", "设置报时类型", "设置报时者名字" },
            defaulted = true,
            robotEvents = { RobotEventEnum.FRIEND_MSG, RobotEventEnum.GROUP_MSG })
    public PluginResult messageHandler(PluginParam pluginParam) {
        // 接收消息
        String message = String.valueOf(pluginParam.getData());
        // 获取配置
        AiRepltSetting aiRepltSetting = getHookSetting(AiRepltSetting.class);
        if (aiRepltSetting == null) {
            aiRepltSetting = new AiRepltSetting();
        }
        // 实例化回复对象
        PluginResult pluginResult = new PluginResult();
        if (ExecuteTriggerEnum.KEYWORD.equals(pluginParam.getExecuteTriggerEnum())) {
            if (Arrays.asList(getAdmins()).contains(pluginParam.getFrom())) {
                if ("设置聊天前缀".equals(pluginParam.getKeyword())) {
                    pluginResult.setProcessed(true);
                    pluginResult.setHold(true);
                    pluginResult.setMessage("请在下条消息中告诉我前缀内容");
                } else if ("设置报时类型".equals(pluginParam.getKeyword())) {
                    pluginResult.setProcessed(true);
                    pluginResult.setHold(true);
                    pluginResult.setMessage("请回复编号（1：所有群，2：仅白名单内的群）");
                } else if ("设置报时者名字".equals(pluginParam.getKeyword())) {
                    pluginResult.setProcessed(true);
                    pluginResult.setHold(true);
                    pluginResult.setMessage("请在下条消息中告诉我报时者名字");
                }
            } else {
                pluginResult.setProcessed(true);
                pluginResult.setMessage("你没有权限操作");
            }
            return pluginResult;
        } else if (ExecuteTriggerEnum.HOLD.equals(pluginParam.getExecuteTriggerEnum())) {
            pluginResult.setProcessed(true);
            if ("设置聊天前缀".equals(pluginParam.getHoldMessage())) {
                aiRepltSetting.setPrefix(message);
                pluginResult.setMessage("聊天前缀已经设置为：".concat(message));
            } else if ("设置报时类型".equals(pluginParam.getHoldMessage())) {
                if (message.equals("1")) {
                    aiRepltSetting.setTimerType(1);
                    pluginResult.setMessage("已修改为给所有群报时");
                } else if (message.equals("2")) {
                    aiRepltSetting.setTimerType(2);
                    pluginResult.setMessage("已修改为仅给白名单内的群报时");
                } else {
                    pluginResult.setMessage("设置无效");
                }
            } else if ("设置报时者名字".equals(pluginParam.getHoldMessage())) {
                aiRepltSetting.setTimerName(message);
                pluginResult.setMessage("报时者名字已经设置为：".concat(message));
            }
            // 保存配置
            saveHookSetting(aiRepltSetting);
            return pluginResult;
        }
        if (pluginParam.getAts().size() >= 10) {
            pluginResult.setMessage("涉及违规At消息，已撤回此消息，并禁言30天，请通知管理员处理！！");
            pluginResult.setAction(new MuteAndRecallAction(pluginParam.getFrom()));
            pluginResult.setProcessed(true);
            return pluginResult;
        }

        // 获取聊天前缀
        String prefix = aiRepltSetting.getPrefix();
        if (RobotEventEnum.GROUP_MSG.equals(pluginParam.getRobotEventEnum())
                && StrUtil.isNotEmpty(prefix)
                && !StrUtil.startWith(message, prefix)) {
            pluginResult.setProcessed(true);
            return pluginResult;
        } else {
            String reply = "这是啥？";
            if (!StringUtils.isEmpty(message)) {
                String regEx = "[a-z0-9A-Z\\u4e00-\\u9fa5]";
                Pattern p = Pattern.compile(regEx);
                Matcher m = p.matcher(message);
                StringBuilder sb = new StringBuilder();
                while (m.find()) {
                    sb.append(m.group());
                }
                message = StringUtils.isEmpty(sb.toString()) ? "啥表情" : sb.toString();
                reply = aiReply(message, prefix);
            }
            pluginResult.setProcessed(true);
            pluginResult.setMessage(reply);
        }
        return pluginResult;
    }

    @PJob(cron = "0 0 * * * ?", hookName = "AiReply")
    public void handlerTimer() {
        // 获取配置
        AiRepltSetting aiRepltSetting = getHookSetting(AiRepltSetting.class);
        MessageEvent messageEvent = getMessageEvent(aiRepltSetting);
        if (CollUtil.isNotEmpty(messageEvent.getToIds())) {
            Integer hour = LocalTime.now().getHour();
            String name = aiRepltSetting == null || StrUtil.isBlank(aiRepltSetting.getTimerName()) ? "茉莉" : aiRepltSetting.getTimerName();
            String resultMsg = getTipByHour(hour, name);
            if (StringUtils.isEmpty(resultMsg)) {
                return;
            }
            messageEvent.setMessage(resultMsg);
            pushMessage(messageEvent);
        }
    }

    private String aiReply(String message, String prefix) {
        if (StrUtil.isNotEmpty(prefix) && StrUtil.startWith(message, prefix)) {
            message = message.substring(prefix.length());
        }
        String aiUrl = String.format("http://i.itpk.cn/api.php?question=%s&api_key=%s&api_secret=%s", message, apiKey, apiSecret);
        return restTemplate.getForObject(aiUrl, String.class);
    }

//    @PJob(cron = "0 0 7 * * ? ", hookName = "AiReply")
//    public void timeOffMuteAll(){
//        // 获取配置
//        AiRepltSetting aiRepltSetting = getHookSetting(AiRepltSetting.class);
//        MessageEvent messageEvent = getMessageEvent(aiRepltSetting);
//        if (!CollectionUtils.isEmpty(messageEvent.getToIds())) {
//            Integer hour = LocalTime.now().getHour();
//            messageEvent.setAction(Action.builder().isMuteAll(false).build());
//            messageEvent.setMessage(String.format("%s宵禁结束！", hour));
//            pushMessage(messageEvent);
//        }
//    }
//    @PJob(cron = "0 0 0 * * ? ", hookName = "AiReply")
//    public void timeOnMuteAll(){
//        // 获取配置
//        AiRepltSetting aiRepltSetting = getHookSetting(AiRepltSetting.class);
//        MessageEvent messageEvent = getMessageEvent(aiRepltSetting);
//        if (!CollectionUtils.isEmpty(messageEvent.getToIds())) {
//            Integer hour = LocalTime.now().getHour();
//            messageEvent.setAction(Action.builder().isMuteAll(true).build());
//            messageEvent.setMessage(String.format("%s点开始宵禁！", hour));
//            pushMessage(messageEvent);
//        }
//    }
    /**
     * 根据当前小时获取提示语
     *
     * @param hour
     * @return
     */
    private String getTipByHour(Integer hour, String name) {
        String tip = "";
        switch (hour) {
            case 0:
                tip = "穿过挪威的森林，让我走进你梦里，夕阳落在我的铠甲，王子不一定骑白马，黑马王子四海为家。\r\n我是" + name + "，现在是凌晨十二点。";
                break;
//            case 1:
//                tip = "凌晨一点了，你还瞪起眼睛像铜铃，如果实在睡不着，那就来找我聊天试试。";
//                break;
//            case 2:
//                tip = "凌晨两点了你咋个还没睡哦，这个世界上有很多爱你的人，所以你也要好好爱你个人哦。";
//                break;
//            case 3:
//                tip = "所有在梦想面前受的伤都可以忽略不计，如果我做得到，那你也可以，现在凌晨三点钟。";
//                break;
//            case 4:
//                tip = "上帝关上门肯定会为你开窗，记得有我给你们加油打气，我是" + name + "，现在是凌晨四点整。";
//                break;
//            case 5:
//                tip = "你的人生目标找到没，是不是还觉得世界不公平，不要害怕，有一天你也会闪闪发光，现在是清晨5点。";
//                break;
//            case 6:
//                tip = "我们用爱和希望来给世界做点缀，走在沙漠也会像在花丛中一样，现在是早上六点。";
//                break;
            case 7:
                tip = "现在是早上7点，" + name + "问候你早安。\r\n早餐是大脑活动的能量之源，请勿空腹开启一天的工作，早餐您吃了吗？";
                break;
            case 8:
                tip = "天生我才那就必定有用，只要你还有梦，总有一天墙角也会盛开花，我是" + name + "，现在是早上八点，开启新的一天，掌声送给还在奋斗中的你。";
                break;
            case 9:
                tip = "永远相信自己不一般，不管他们喜不喜欢，所以坚持你的道路勇往直前，现在是九点整，我是" + name;
                break;
            case 10:
                tip = String.format("我是%s，现在是上午十点，工作很忙很累，但是每个人都会累，没人能为你承担所有伤悲，人总有一段时间要学会自己长大。", name);
                break;
            case 11:
                tip = "现在是上午十一点，工作半天了，" + name + "邀请您从电脑上移开视线，站起来伸个懒腰，看看窗外，快乐工作，快乐生活。";
                break;
            case 12:
                tip = "现在是午间十二点，享受午餐，享受放松时光，" + name + "祝你午餐好胃口。";
                break;
            case 13:
                tip = "现在是午后一点，" + name + "提醒您休息片刻，下午的工作更有精神。";
                break;
            case 14:
                tip = "现在是午后两点，开始下午的工作哦，" + name + "与你一起努力。";
                break;
            case 15:
                tip = String.format("事都有着多面性，换个方向，或许你能发现更多答案。现在是下午三点；我是%s", name);
                break;
            case 16:
                tip = "不是每个人生来就是白马王子，但是这有啥关系喃，巴巴掌送给正在努力中的你，我是" + name + "，现在是下午四点。";
                break;
            case 17:
                tip = String.format("现在是下午五点。揉一揉太阳穴，暂时放下案头的工作，来一杯咖啡，放松一下心情吧。我是%s", name);
                break;
            case 18:
                tip = "现在是下午六点，" + name + "提示正在回家路上的你，注意安全，安全是亲人的期待。";
                break;
            case 19:
                tip = "现在是晚上七点，" + name + "祝您晚餐好胃口，陪伴家人，享受亲情，幸福其实很简单。";
                break;
            case 20:
                tip = "现在是晚上八点，无论是家人在一起，还是朋友在一起，" + name + "倡议放下手机，让我们的情感回归本源。";
                break;
            case 21:
                tip = "现在是晚间九点，灯火璀璨，城市在夜色中绽放，" + name + "与你共享美丽夜色。";
                break;
            case 22:
                tip = "现在是晚间十点，喧嚣退去，城市开始安静的呼吸。\r\n" + name + "提醒你早睡早起，有益身体的健康。";
                break;
            case 23:
                tip = "现在是晚间十一点，夜色深沉，繁星点点下的美丽城市，" + name + "与您走向梦的香甜！";
                break;
            default:
                tip = "";
                break;
        }
        return tip;
    }

    public MessageEvent getMessageEvent(AiRepltSetting aiRepltSetting) {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setRobotEventEnum(RobotEventEnum.GROUP_MSG);

        if (aiRepltSetting == null
                || aiRepltSetting.getTimerType() == null
                || aiRepltSetting.getTimerType() == 1) {
            // 获取所有群列表
            List<RobotDef.Group> getGroupList = getGroupList();
            // 整点报时发给所有群
            messageEvent.setToIds(getGroupList.stream().map(RobotDef.Group::getId).collect(Collectors.toList()));
        } else {
            // 获取所有的白名单群ID列表
            List<String> groupIdList = getGroupIdAllowList();
            // 整点报时发给所有群
            messageEvent.setToIds(groupIdList);
        }
        return messageEvent;
    }
}