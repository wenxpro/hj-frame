package com.wenx.base.rest;

import com.wenx.base.domain.BaseDto;
import com.wenx.base.domain.BaseQuery;
import com.wenx.v3core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

public abstract class BaseReadController <D extends BaseDto, Q extends BaseQuery, S extends BaseReadService<D, Q>> {

    protected S service;

    protected BaseReadController(S service) {
        this.service = service;
    }


    @PostMapping("/page")
    @Operation(summary = "分页")
    public R page(Q q) {
        return R.ok(service.page(q));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取")
    public R get(@PathVariable Serializable id) {
        return R.ok(service.get(id));
    }
}
