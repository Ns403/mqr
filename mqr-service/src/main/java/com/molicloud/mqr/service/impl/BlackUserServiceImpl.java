package com.molicloud.mqr.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.molicloud.mqr.entity.BlackUserDto;
import com.molicloud.mqr.mapper.BlackUserMapper;
import com.molicloud.mqr.service.BlackUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ns
 */
@Service
public class BlackUserServiceImpl implements BlackUserService {
    @Autowired
    private BlackUserMapper blackUserMapper;

    @Override
    public int addBlackUserList(List<String> ids) {
        List<BlackUserDto> list = blackUserMapper.selectList(Wrappers.<BlackUserDto>lambdaQuery().in(BlackUserDto::getQq, ids));
        if (CollectionUtil.isNotEmpty(list)) {
            blackUserMapper.updateBlackRecord(list);
            Set<String> existQq = list.stream().map(BlackUserDto::getQq).collect(Collectors.toSet());
            ids.removeAll(existQq);
        }
        if (CollectionUtil.isNotEmpty(ids)) {
            return blackUserMapper.addBlackRecord(ids);
        }
        return 0;
    }

    @Override
    public Boolean checkUserInBlack(String fromQq) {
        BlackUserDto blackUserDto = blackUserMapper.selectOne(Wrappers.<BlackUserDto>lambdaQuery().eq(BlackUserDto::getQq, fromQq));
        return Objects.nonNull(blackUserDto);
    }

}
