package com.molicloud.mqr.plugin.core.action;

import lombok.*;

import java.util.List;

/**
 * 插件钩子的动作
 *
 * @author feitao yyimba@qq.com
 * @since 2020/11/17 10:42 上午
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Action {

    /**
     * 操作的ID列表（QQ号）
     */
    private List<String> ids;
    /**
     * 是否开启全体禁言
     */
    private Boolean isMuteAll;
}