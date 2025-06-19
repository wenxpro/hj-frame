package com.wenx.base.rest;

import com.wenx.base.domain.BaseDto;
import com.wenx.base.domain.BaseQuery;

import java.io.Serializable;

public interface BaseRestService<D extends BaseDto, P extends BaseQuery>
        extends BaseReadService<D, P>{

    void add(D dto);

    void delete(Serializable id);

    void update(D dto);
}
