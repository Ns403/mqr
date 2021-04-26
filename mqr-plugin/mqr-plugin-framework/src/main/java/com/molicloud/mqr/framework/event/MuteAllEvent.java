package com.molicloud.mqr.framework.event;

import net.mamoe.mirai.event.BroadcastControllable;

/**
 * @author Ns
 */
public class MuteAllEvent implements BroadcastControllable {

    @Override
    public boolean getShouldBroadcast() {
        return false;
    }

    @Override
    public boolean isIntercepted() {
        return false;
    }

    @Override
    public void intercept() {

    }
}
