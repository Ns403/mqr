package com.molicloud.mqr.plugin.mute.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Ns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMuteAllDao {
    /**
     * id
     */
    private Integer id;
    /**
     * 群id
     */
    private String groupId;
    /**
     * 全体禁言状态
     * 0未开启 1开启全体禁言
     */
    private Integer muteAllStatus;
    /**
     * 自动禁言开关
     * 0未开启自动禁言 1开启自动禁言
     */
    private Integer autoMuteAllStatus;

}
