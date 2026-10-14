package com.itservice.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.model.Department;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentMapper departmentMapper;

    public DepartmentController(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    /**
     * 科室列表（供科室端三级联动：院区 → 楼栋 → 科室）。
     * 固定按 院区 → 楼栋 → id 升序返回，保证前端分组顺序稳定。
     */
    @GetMapping
    public ApiResponse list() {
        List<Department> list = departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getCampus)
                .orderByAsc(Department::getBuilding)
                .orderByAsc(Department::getId));
        return ApiResponse.ok(list);
    }
}