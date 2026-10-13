package com.itservice.desk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itservice.desk.model.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}