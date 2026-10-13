package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.WorkOrderCreateRequest;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final AtomicLong assignCursor = new AtomicLong(0);

    public WorkOrderService(WorkOrderMapper workOrderMapper,
                            DepartmentMapper departmentMapper,
                            UserMapper userMapper) {
        this.workOrderMapper = workOrderMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    public WorkOrder createWorkOrder(WorkOrderCreateRequest req, Long creatorId) {
        Department dept = departmentMapper.selectById(req.getDepartmentId());
        if (dept == null) {
            throw new IllegalArgumentException("科室不存在");
        }
        WorkOrder wo = new WorkOrder();
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setDepartmentId(dept.getId());
        wo.setDepartmentName(dept.getName());
        wo.setCampus(dept.getCampus());
        wo.setLocation(req.getLocation() == null ? dept.getBuilding() : req.getLocation());
        wo.setContactPerson(req.getContactPerson());
        wo.setContactPhone(req.getContactPhone());
        wo.setPriority(req.getPriority() == null ? "MEDIUM" : req.getPriority());
        wo.setStatus(WorkOrderStatus.ASSIGNED);
        wo.setAssigneeId(nextRepairerId());
        wo.setCreatorId(creatorId);
        wo.setCreatedAt(LocalDateTime.now());
        workOrderMapper.insert(wo);
        return wo;
    }

    private Long nextRepairerId() {
        List<User> repairers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.REPAIRER)
                .orderByAsc(User::getId));
        if (repairers.isEmpty()) {
            throw new IllegalStateException("没有可用的维修人员");
        }
        long idx = assignCursor.getAndIncrement() % repairers.size();
        return repairers.get((int) idx).getId();
    }

    public List<WorkOrder> listTodayTasks(Long assigneeId) {
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAssigneeId, assigneeId)
                .in(WorkOrder::getStatus, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS)
                .orderByDesc(WorkOrder::getCreatedAt));
    }

    public WorkOrder getById(Long id) {
        WorkOrder wo = workOrderMapper.selectById(id);
        if (wo == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return wo;
    }

    public WorkOrder submitRepair(Long id, String record) {
        WorkOrder wo = getById(id);
        wo.setStatus(WorkOrderStatus.COMPLETED);
        wo.setRepairRecord(record);
        wo.setCompletedAt(LocalDateTime.now());
        workOrderMapper.updateById(wo);
        return wo;
    }
}