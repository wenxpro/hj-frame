package com.wenx.base.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * base entity 公共字段
 *
 * @author wenx
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -4314012298679690170L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
    /**
     * 修改者
     */
    @TableField(value = "modify_by", fill = FieldFill.INSERT_UPDATE)
    private Long modifyBy;
    /**
     * 修改时间
     */
    @TableField(value = "modify_time", fill = FieldFill.INSERT_UPDATE)
    private Date modifyTime;

    /**
     * 版本
     */
    @TableField(value = "version", fill = FieldFill.INSERT_UPDATE)
    @Version
    private Integer version;

    /**
     * 逻辑删除
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    @TableLogic
    private Integer deleted;


}