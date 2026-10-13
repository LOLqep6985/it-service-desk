package com.itservice.desk.service;

import com.itservice.desk.dto.WorkOrderCreateRequest;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import com.itservice.desk.repository.InMemoryStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private final InMemoryStore store;
    private final AtomicLong assignCursor = new AtomicLong(0);

    public WorkOrderService(InMemoryStore store) {
        this.store = store;
    }

    public WorkOrder createWorkOrder(WorkOrderCreateRequest req, Long creatorId) {
        Department dept = store.departments.get(req.getDepartmentId());
        if (dept == null) {
            throw new IllegalArgumentException("科室不存在");
        }
        WorkOrder wo = new WorkOrder();
        wo.setId(store.orderIdSeq.getAndIncrement());
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
        store.workOrders.put(wo.getId(), wo);
        return wo;
    }

    private Long nextRepairerId() {
        List<User> repairers = store.users.values().stream()
                .filter(u -> u.getRole() == UserRole.REPAIRER)
                .sorted(Comparator.comparing(User::getId))
                .collect(Collectors.toList());
        if (repairers.isEmpty()) {
            throw new IllegalStateException("没有可用的维修人员");
        }
        long idx = assignCursor.getAndIncrement() % repairers.size();
        return repairers.get((int) idx).getId();
    }

    public List<WorkOrder> listTodayTasks(Long assigneeId) {
        return store.workOrders.values().stream()
                .filter(wo -> assigneeId.equals(wo.getAssigneeId()))
                .filter(wo -> wo.getStatus() == WorkOrderStatus.ASSIGNED
                        || wo.getStatus() == WorkOrderStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(WorkOrder::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public WorkOrder getById(Long id) {
        WorkOrder wo = store.workOrders.get(id);
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
        return wo;
    }
}