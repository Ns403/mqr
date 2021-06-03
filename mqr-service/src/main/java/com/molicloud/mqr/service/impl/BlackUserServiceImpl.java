package com.molicloud.mqr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.molicloud.mqr.entity.BlackUserDto;
import com.molicloud.mqr.mapper.BlackUserMapper;
import com.molicloud.mqr.service.BlackUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author Ns
 */
@Service
public class BlackUserServiceImpl implements BlackUserService {
    @Autowired
    private BlackUserMapper blackUserMapper;

    @Override
    public int addBlackUserList(List<String> ids) {
        return blackUserMapper.addBlackRecord(ids);
    }

    @Override
    public Boolean checkUserInBlack(String fromQq) {
        BlackUserDto blackUserDto = blackUserMapper.selectOne(Wrappers.<BlackUserDto>lambdaQuery().eq(BlackUserDto::getQq, fromQq));
        return Objects.nonNull(blackUserDto);
    }

}
