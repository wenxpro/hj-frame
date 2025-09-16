package com.wenx.base.rest;

import com.wenx.base.SelectiveUpdateHelper;
import com.wenx.base.domain.BaseDto;
import com.wenx.base.domain.BaseEntity;
import com.wenx.base.domain.BaseQuery;

import java.io.Serializable;

public interface BaseRestService<D extends BaseDto, P extends BaseQuery>
        extends BaseReadService<D, P>{

    void add(D dto);

    void delete(Serializable id);

    void update(D dto);
    
    /**
     * 选择性更新，只更新非空字段
     * 
     * @param entity PO 实体对象，只有非空字段会被更新
     */
    default void updateBySelect(BaseEntity entity) {
        // 检查服务实例类型
        if (!(this instanceof com.baomidou.mybatisplus.extension.service.IService)) {
            throw new UnsupportedOperationException("updateBySelect 需要 MyBatis-Plus IService 支持");
        }
        
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.service.IService<?> service = 
            (com.baomidou.mybatisplus.extension.service.IService<?>) this;
        
        // 委托给工具类处理
        SelectiveUpdateHelper.updateBySelect(service, entity);
    }
    
    /**
     * 选择性保存，根据ID是否存在决定新增或更新，只处理非空字段
     * 
     * @param entity PO 实体对象，只有非空字段会被保存或更新
     */
    default void saveBySelective(BaseEntity entity) {
        // 检查服务实例类型
        if (!(this instanceof com.baomidou.mybatisplus.extension.service.IService)) {
            throw new UnsupportedOperationException("saveBySelective 需要 MyBatis-Plus IService 支持");
        }
        
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.service.IService<?> service = 
            (com.baomidou.mybatisplus.extension.service.IService<?>) this;
        
        // 委托给工具类处理
        SelectiveUpdateHelper.saveBySelect(service, entity);
    }
}
