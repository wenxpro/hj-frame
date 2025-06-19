package com.wenx.base.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wenx.base.domain.BaseDto;
import com.wenx.base.domain.BaseQuery;

import java.io.Serializable;

public interface BaseReadService<D extends BaseDto, P extends BaseQuery> {

    IPage<?> page(P page);

    Object get(Serializable id);
}
