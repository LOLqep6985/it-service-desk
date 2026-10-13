package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.dto.ConsumableItem;
import com.itservice.desk.dto.WorkOrderCreateRequest;
import com.itservice.desk.mapper.ConsumableMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final ConsumableMapper consumableMapper;
    private final AtomicLong assignCursor = new AtomicLong(0);

    public WorkOrderService(WorkOrderMapper workOrderMapper,
                            DepartmentMapper departmentMapper,
                            UserMapper userMapper,
                            ConsumableMapper consumableMapper) {
        this.workOrderMapper = workOrderMapper;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.consumableMapper = consumableMapper;
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
        wo.setSignStatus(SignStatus.UNSIGNED);
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

    public List<WorkOrder> listAll(String status) {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<WorkOrder>()
                .orderByDesc(WorkOrder::getCreatedAt);
        if (status != null && !status.isBlank()) {
            qw.eq(WorkOrder::getStatus, status);
        }
        return workOrderMapper.selectList(qw);
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

    public WorkOrder accept(Long id) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.ASSIGNED) {
            throw new IllegalArgumentException("只有已派单的工单可以接单");
        }
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrderMapper.updateById(wo);
        return wo;
    }

    public List<WorkOrder> listPendingAcceptByDepartment(Long departmentId) {
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getDepartmentId, departmentId)
                .eq(WorkOrder::getStatus, WorkOrderStatus.PENDING_ACCEPT)
                .orderByDesc(WorkOrder::getCreatedAt));
    }

    public WorkOrder sign(Long id, String signerName, boolean isDelegate, String delegatedName, String signatureUrl) {
        WorkOrder wo = getById(id);
        if (wo.getStatus() != WorkOrderStatus.PENDING_ACCEPT) {
            throw new IllegalArgumentException("该工单不在待验收状态");
        }
        wo.setStatus(WorkOrderStatus.COMPLETED);
        wo.setCompletedAt(LocalDateTime.now());
        wo.setSignStatus(SignStatus.SIGNED);
        if (isDelegate && delegatedName != null && !delegatedName.isBlank()) {
            wo.setSignedBy(delegatedName + "(由" + signerName + "代签)");
        } else {
            wo.setSignedBy(signerName);
        }
        wo.setSignedAt(LocalDateTime.now());
        wo.setSignatureUrl(signatureUrl);
        workOrderMapper.updateById(wo);
        return wo;
    }
    public List<Consumable> listConsumables(Long workOrderId) {
        return consumableMapper.selectList(new LambdaQueryWrapper<Consumable>()
                .eq(Consumable::getWorkOrderId, workOrderId)
                .orderByAsc(Consumable::getId));
    }
    public WorkOrder submitRepair(Long id, String record, List<ConsumableItem> consumables) {
        WorkOrder wo = getById(id);
        wo.setStatus(WorkOrderStatus.PENDING_ACCEPT);
        wo.setRepairRecord(record);
        workOrderMapper.updateById(wo);
        if (consumables != null) {
            for (ConsumableItem item : consumables) {
                if (item.getName() == null || item.getName().isBlank()) {
                    continue;
                }
                Consumable c = new Consumable();
                c.setWorkOrderId(wo.getId());
                c.setName(item.getName());
                c.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity());
                c.setUnitPrice(item.getUnitPrice());
                c.setIsIou(Boolean.TRUE.equals(item.getIsIou()));
                c.setIouNo(item.getIouNo());
                consumableMapper.insert(c);
            }
        }
        return wo;
    }
}