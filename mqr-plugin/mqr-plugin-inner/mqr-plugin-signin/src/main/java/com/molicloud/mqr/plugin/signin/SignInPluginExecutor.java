package com.molicloud.mqr.plugin.signin;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.molicloud.mqr.plugin.core.AbstractPluginExecutor;
import com.molicloud.mqr.plugin.core.PluginInfo;
import com.molicloud.mqr.plugin.core.PluginParam;
import com.molicloud.mqr.plugin.core.PluginResult;
import com.molicloud.mqr.plugin.core.annotation.PHook;
import com.molicloud.mqr.plugin.core.define.FaceDef;
import com.molicloud.mqr.plugin.core.enums.RobotEventEnum;
import com.molicloud.mqr.plugin.core.message.MessageBuild;
import com.molicloud.mqr.plugin.core.message.make.Ats;
import com.molicloud.mqr.plugin.core.message.make.Expression;
import com.molicloud.mqr.plugin.core.message.make.Text;
import com.molicloud.mqr.plugin.core.util.DateUtil;
import com.molicloud.mqr.plugin.signin.entity.RobotPluginSignIn;
import com.molicloud.mqr.plugin.signin.mapper.RobotPluginSignInMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 签到插件
 *
 * @author wispx wisp-x@qq.com
 * @since 2020/12/03 2:44 下午
 */
@Slf4j
@Component
public class SignInPluginExecutor extends AbstractPluginExecutor {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RobotPluginSignInMapper mapper;

    @PHook(name = "SignIn", equalsKeywords = {
            "签到",
    }, robotEvents = {
            RobotEventEnum.GROUP_MSG,
    })
    public PluginResult messageHandler(PluginParam pluginParam) {
        PluginResult pluginResult = new PluginResult();
        MessageBuild messageBuild = new MessageBuild();
        pluginResult.setProcessed(true);
        //查询出是否有打卡记录
        RobotPluginSignIn signInRecord = getNewSignInRecord(pluginParam.getTo(), pluginParam.getFrom());
        Ats ats = new Ats();
        ats.setMids(Lists.newArrayList(pluginParam.getFrom()));
        messageBuild.append(ats);
        //没有为首次签到
        if (Objects.isNull(signInRecord)) {
            String hitokoto = hitokoto();
            RobotPluginSignIn signInLog = RobotPluginSignIn.builder()
                    .qq(pluginParam.getFrom())
                    .groupId(pluginParam.getTo())
                    .isContinuity(false)
                    .num(1)
                    .updateTime(DateUtil.nowFormatLocalDate()).build();
            MessageBuild resultBuild = getResultBuild(signInLog, pluginParam, messageBuild, hitokoto);
            mapper.insert(signInLog);
            pluginResult.setMessage(resultBuild);
            return pluginResult;
        }
        //有判断是不是今天签到
        LocalDate localDate = DateUtil.parseLocalDate(signInRecord.getUpdateTime());
        LocalDate now = LocalDate.now();

        if (now.equals(localDate)) {
            ats.setContent("你今天已经签到过啦，明儿再来吧～");
            pluginResult.setMessage(messageBuild);
        } else {
            String hitokoto = hitokoto();
            boolean continuousSign = now.minusDays(1).equals(localDate);
            signInRecord = RobotPluginSignIn.builder()
                    .id(signInRecord.getId())
                    .num(continuousSign ? signInRecord.getNum() + 1 : 1)
                    .isContinuity(continuousSign).build();
            MessageBuild resultBuild = getResultBuild(signInRecord, pluginParam, messageBuild, hitokoto);
            pluginResult.setMessage(resultBuild);
            signInRecord.setUpdateTime(DateUtil.nowFormatLocalDate());
            mapper.updateById(signInRecord);
        }
        return pluginResult;
    }

    /**
     * 构建饭回消息
     *
     * @param signIn       保存
     * @param pluginParam  参数
     * @param messageBuild 构建返回消息
     * @return 构建饭回
     */
    private MessageBuild getResultBuild(RobotPluginSignIn signIn, PluginParam pluginParam, MessageBuild messageBuild, String hitokoto) {
        String content = "";
        int hour = LocalDateTime.now().getHour();
        if (hour <= 6) {
            content = "早上好，今天又是充满元气的一天喔～\r\n签到成功～，今天你是第 %d 个签到的哟～)\n";
        }
        if (hour > 6 && hour <= 12) {
            content = "上午好，今天有多喝水吗？\r\n签到成功～，今天你是第 %d 个签到的哟～)\n";
        }
        if (hour == 13) {
            content = "中午好，午饭要吃的饱饱的，要记得午睡喔～\r\n签到成功～，今天你是第 %d 个签到的哟～)\n";
        }
        if (hour > 13 && hour <= 18) {
            content = "下午好，一天已经过去一半啦！\r\n签到成功～，今天你是第 %d 个签到的哟～)\n";
        }
        if (hour > 18) {
            content = "晚上好，早点休息鸭～\r\n签到成功～，今天你是第 %d 个签到的哟～)\n";
        }
        Integer todaySignInCnt = getTodaySignInCnt(pluginParam.getTo());
        String signStr = String.format(content, todaySignInCnt + 1);
        messageBuild.append(new Text(signStr));
        messageBuild.append(new Expression(FaceDef.meigui));
        if (signIn.getIsContinuity()) {
            //如果超出昨天算没有签
            String signCntStr = String.format("\r\n截止今日，你已经连续签到 %d 天啦！明天还要继续加油鸭～", signIn.getNum());
            messageBuild.append(new Text(signCntStr));
        }
        // 一言
        messageBuild.append(new Text(String.format("\r\n今日份鸡汤「%s」", hitokoto)));
        return messageBuild;
    }

    /**
     * 获取发送者最新的打卡记录
     *
     * @param groupId qq群
     * @param qq      发送者
     * @return 记录
     */
    private RobotPluginSignIn getNewSignInRecord(String groupId, String qq) {
        return mapper.selectOne(Wrappers.<RobotPluginSignIn>lambdaQuery()
                .eq(RobotPluginSignIn::getGroupId, groupId)
                .eq(RobotPluginSignIn::getQq, qq)
                .orderByDesc(RobotPluginSignIn::getUpdateTime));

    }

    /**
     * 获取发送者最新的打卡记录
     *
     * @param groupId qq群
     * @return 记录
     */
    private Integer getTodaySignInCnt(String groupId) {
        return mapper.selectCount(Wrappers.<RobotPluginSignIn>lambdaQuery()
                .eq(RobotPluginSignIn::getGroupId, groupId)
                .ge(RobotPluginSignIn::getUpdateTime, LocalDate.now()));
    }

    /**
     * 一言
     *
     * @return
     */
    private String hitokoto() {
        String content = "";
        try {
            content = restTemplate.getForObject("https://v1.hitokoto.cn/?c=d&encode=text", String.class);
        } catch (RestClientException e) {
            content = "哎呀，今日没有鸡汤~";
        }
        return content;
    }

    @Override
    public PluginInfo getPluginInfo() {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setName("签到插件");
        pluginInfo.setAuthor("WispX");
        pluginInfo.setHomeUrl("https://github.com/wisp-x");
        pluginInfo.setExplain("群签到插件，支持连续签到、签到后发送一言");
        pluginInfo.setVersion(10001);
        pluginInfo.setInitScript(
                "CREATE TABLE \"robot_plugin_signin\" (" +
                        "  \"id\" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "  \"group_id\" VARCHAR(50) NOT NULL," +
                        "  \"qq\" VARCHAR(50) NOT NULL," +
                        "  \"is_continuity\" BIT(1) NOT NULL DEFAULT 0," +
                        "  \"num\" INTEGER NOT NULL DEFAULT 0," +
                        "  \"update_time\" VARCHAR NOT NULL " +
                        ");");
        return pluginInfo;
    }
}
