package com.alibaba.cloud.ai.example.dcg.server.service.impl;

import com.alibaba.cloud.ai.example.dcg.server.entity.Field;
import com.alibaba.cloud.ai.example.dcg.server.mapper.FieldMapper;
import com.alibaba.cloud.ai.example.dcg.server.service.IFieldService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author HY-love-sleep
 * @since 2025-04-15
 */
@Service
public class FieldServiceImpl extends ServiceImpl<FieldMapper, Field> implements IFieldService {

    @Override
    public Field getFieldByName(String name) {
        LambdaQueryWrapper<Field> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Field::getFieldName, name);
        return this.getOne(lqw);
    }
}
