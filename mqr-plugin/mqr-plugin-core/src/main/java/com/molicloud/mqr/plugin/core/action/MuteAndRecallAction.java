package com.molicloud.mqr.plugin.core.action;

import com.beust.jcommander.internal.Lists;
import lombok.Data;
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
