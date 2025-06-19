package com.wenx.base.rest;

import com.wenx.base.domain.BaseDto;
import com.wenx.base.domain.BaseQuery;
import com.wenx.v3core.response.R;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

public abstract class BaseRestController<D extends BaseDto, Q extends BaseQuery, S extends BaseRestService<D, Q>>
        extends BaseReadController<D, Q, S> {

    protected BaseRestController(S service) {
        super(service);
    }

    @PostMapping("/add")
    @Operation(summary = "添加")
    public R add(@RequestBody D dto) {
        service.add(dto);
        return R.ok();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除")
    public R delete(@PathVariable Serializable id) {
        service.delete(id);
        return R.ok();
    }

    @PutMapping("/update")
    @Operation(summary = "更新")
    public R update(@RequestBody D dto) {
        service.update(dto);
        return R.ok();
    }
}
