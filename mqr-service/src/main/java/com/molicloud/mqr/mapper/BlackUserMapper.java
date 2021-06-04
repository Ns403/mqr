package com.molicloud.mqr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.molicloud.mqr.entity.BlackUserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author nss
 */
@Mapper
public interface BlackUserMapper extends BaseMapper<BlackUserDto> {

    int addBlackRecord(@Param("qqList") List<String> qqList);

    int updateBlackRecord(@Param("list") List<BlackUserDto> list);
}
