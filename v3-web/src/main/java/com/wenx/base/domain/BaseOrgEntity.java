package com.wenx.base.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseOrgEntity extends BaseEntity {


    /**
     * 组织id
     */
    @TableField(value = "org_id", fill = FieldFill.INSERT)
    private Long orgId;
}
