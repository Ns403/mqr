package com.molicloud.mqr.plugin.core.action.impl;

import com.beust.jcommander.internal.Lists;
import com.molicloud.mqr.plugin.core.action.Action;
import lombok.EqualsAndHashCode;

/**
 * @author 10479
 */
@EqualsAndHashCode(callSuper = true)
public class MuteAndRecallAction extends Action {
    public MuteAndRecallAction(String id){
        this.setIds(Lists.newArrayList(id));
    }
}
