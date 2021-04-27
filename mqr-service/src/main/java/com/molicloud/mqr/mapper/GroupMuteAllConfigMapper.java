package com.molicloud.mqr.mapper;

import com.molicloud.mqr.entity.GroupMuteAllDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Ns
 */
@Mapper
public interface GroupMuteAllConfigMapper {
    /**
     * 查询所有
     * @return String
     */
    String getAll();

    /**
     * 根据qq群id查群禁言配置
     * @param groupId 群id
     * @return 群配置
     */
    GroupMuteAllDao selectByGroupId(@Param("groupId") String groupId);

    /**
     * 新增记录
     * @param groupMuteAllDao 新增记录配置
     * @return line
     */
    int insertRecord(GroupMuteAllDao groupMuteAllDao);

    /**
     * 修改记录
     * @param groupMuteAllDao 参数
     * @return line
     */
    int updateRecord(GroupMuteAllDao groupMuteAllDao);


    /**
     * 查询出已配置宵禁的设置
     * @return list
     */
    List<String> selectByAutoMuteAll();
}
