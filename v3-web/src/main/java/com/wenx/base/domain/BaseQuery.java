package com.wenx.base.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 分页 query
 *
 * @author wenx
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(name = "BaseQuery")
public class BaseQuery<E> extends Page<E> implements Serializable {

    @Serial
    private static final long serialVersionUID = -4240945046511385393L;

    @Schema(description = "创建者", name = "createBy")
    private Long createBy;
    @Schema(description = "创建时间", name = "createTime")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开始时间", name = "startTime")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结束时间", name = "endTime")
    private Date endTime;
}