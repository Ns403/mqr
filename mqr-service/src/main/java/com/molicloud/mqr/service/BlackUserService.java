package com.molicloud.mqr.service;

import java.util.List;

/**
 * @author Ns
 */
public interface BlackUserService {
    int addBlackUserList(List<String> ids);

    Boolean checkUserInBlack(String from);
}
