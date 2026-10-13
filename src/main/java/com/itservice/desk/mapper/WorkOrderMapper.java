package com.itservice.desk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itservice.desk.model.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}