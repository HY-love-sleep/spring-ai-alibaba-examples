package com.alibaba.cloud.ai.example.dcg.server.service;

import com.alibaba.cloud.ai.example.dcg.server.entity.Field;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author HY-love-sleep
 * @since 2025-04-15
 */
public interface IFieldService extends IService<Field> {
    Field getFieldByName(String name);
}
