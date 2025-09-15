package com.wenx.base.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseDeptEntity extends BaseEntity {


    /**
     * 部门id
     */
    @TableField(value = "department_id", fill = FieldFill.INSERT)
    private Long departmentId;
}
