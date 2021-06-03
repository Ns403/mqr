package com.molicloud.mqr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * @author nss
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "robot_plugin_black_user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlackUserDto implements Serializable {

    private static final long serialVersionUID = -829733766098954037L;

    /**
     * id 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 黑名单qq
     */
    @TableField(value = "qq")
    private String qq;
    /**
     * 状态 1生效 0失效
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;


}