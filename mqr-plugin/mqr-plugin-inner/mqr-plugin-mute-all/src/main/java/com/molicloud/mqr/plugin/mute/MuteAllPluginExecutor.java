package com.molicloud.mqr.plugin.mute;

import com.molicloud.mqr.plugin.core.*;
import com.molicloud.mqr.plugin.core.action.Action;
import com.molicloud.mqr.plugin.core.annotation.PHook;
import com.molicloud.mqr.plugin.core.enums.ExecuteTriggerEnum;
import com.molicloud.mqr.plugin.core.enums.RobotEventEnum;
import com.molicloud.mqr.plugin.core.message.MessageBuild;
import com.molicloud.mqr.plugin.core.message.make.Ats;
import com.molicloud.mqr.plugin.mute.common.contants.MuteEnum;
import com.molicloud.mqr.plugin.mute.dao.GroupMuteAllDao;
import com.molicloud.mqr.plugin.mute.mappers.GroupMuteAllConfigMapper;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @author Ns
 */
@Component
public class MuteAllPluginExecutor implements PluginExecutor {
    @Autowired
    private GroupMuteAllConfigMapper groupMuteAllConfigMapper;

    @PHook(name = "muteAllPlugin",
            equalsKeywords = {"全体禁言", "解除全体禁言", "开启宵禁", "关闭宵禁", "管理员已开启全体禁言", "管理员已关闭全体禁言"},
            robotEvents = {RobotEventEnum.GROUP_MSG})
    public PluginResult messageHandler(PluginParam pluginParam) {
        if (!ExecuteTriggerEnum.KEYWORD.check(pluginParam.getExecuteTriggerEnum())) {
            PluginResult pluginResult = new PluginResult();
            pluginResult.setProcessed(false);
            return pluginResult;
        }
        //判断是否是关键词以及是否是管理员发的
        String botQQ = "1047967799";
        //无权限处理
        if (!checkAdmins(pluginParam.getFrom()) && !botQQ.equals(pluginParam.getFrom())) {
            PluginResult<MessageBuild> pluginResult = new PluginResult<>();
            Ats ats = new Ats();
            ats.setMids(Lists.newArrayList(pluginParam.getFrom()));
            ats.setContent("您无权限操作！");
            pluginResult.setProcessed(true);
            MessageBuild messageBuild = new MessageBuild();
            messageBuild.append(ats);
            pluginResult.setMessage(messageBuild);
            return pluginResult;
        }
        //处理对应关键词触发

        PluginResult<String> pluginResult = new PluginResult<>();
        String groupId = pluginParam.getTo();
        //查询表里是否有记录
        int muteAllStatus = 0;
        int autoMuteAllStatus = 0;
        String keyword = pluginParam.getKeyword();
        MuteEnum muteEnum = MuteEnum.valueOf(keyword);
        pluginResult.setProcessed(true);
        Action action = new Action();
        switch (muteEnum) {
            case MUTE_ALL:
                pluginResult.setMessage("已开启全体禁言");
                action.setIsMuteAll(true);
                pluginResult.setAction(action);
                muteAllStatus = 1;
                break;
            case MUTE_ALL_ED:
                pluginResult.setMessage("管理员已开启全体禁言，当前发送的消息将自动撤回");
                muteAllStatus = 1;
                break;
            case UN_MUTE_ALL:
                pluginResult.setMessage("已解除全体禁言");
                action.setIsMuteAll(false);
                pluginResult.setAction(action);
                muteAllStatus = 0;
                break;
            case UN_MUTE_ALL_ED:
                pluginResult.setMessage("管理员已关闭全体禁言");
                muteAllStatus = 0;
                break;
            case AUTO_MUTE_ALL:
                pluginResult.setMessage("此群已已启用宵禁");
                autoMuteAllStatus = 1;
                break;
            case UN_AUTO_MUTE_ALL:
                pluginResult.setMessage("此群已关闭用宵禁");
                autoMuteAllStatus = 0;
                break;
            default:
                pluginResult.setMessage("命令不正确");
                return pluginResult;
        }
        GroupMuteAllDao groupMuteAllDao = groupMuteAllConfigMapper.selectByGroupId(groupId);
        if (Objects.isNull(groupMuteAllDao)) {
            //确认是开启禁言还是关闭禁言状态
            //更新禁言状态
            groupMuteAllDao = GroupMuteAllDao.builder()
                    .groupId(groupId)
                    .muteAllStatus(muteAllStatus)
                    .autoMuteAllStatus(autoMuteAllStatus).build();
            groupMuteAllConfigMapper.insertRecord(groupMuteAllDao);
        } else {
            groupMuteAllDao.setMuteAllStatus(muteAllStatus);
            groupMuteAllDao.setAutoMuteAllStatus(autoMuteAllStatus);
            groupMuteAllConfigMapper.updateRecord(groupMuteAllDao);
        }
        return pluginResult;

    }

    /**
     * 判断人是否事管理员
     *
     * @return true 是
     * false 不是
     */
    private boolean checkAdmins(String from) {
        return Lists.newArrayList(RobotContextHolder.getRobot().getAdmins()).contains(from);
    }

    @Override
    public PluginInfo getPluginInfo() {
        Map<Integer, String> updateMap = Maps.newHashMap(100001, "create unique index u_idx_group_id on group_mute_all_config (group_id);");
        return PluginInfo.builder()
                .name("全体禁言插件")
                .author("Ns")
                .explain("处理全体禁言后相关，及全体禁言失效处理")
                .version(100000)
                .initScript("create table group_mute_all_config (" +
                        " id        INTEGER not null primary key autoincrement," +
                        " group_id  VARCHAR(50) not null," +
                        " mute_all_status smallint default 0," +
                        " auto_mute_all_status smallint default 0" +
                        ");").updateScriptList(updateMap).build();
    }
}
