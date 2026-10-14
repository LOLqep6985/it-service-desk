package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Campus campus;
    private String building;
    /** 楼层号，如 3F；用于科室端三级联动（院区→楼栋→科室）第三级的楼层标签 */
    private String floor;
    private String departmentHead;
}