package com.itservice.desk.controller;

import com.itservice.desk.dto.ApiResponse;
import com.itservice.desk.dto.RepairRequest;
import com.itservice.desk.dto.WorkOrderCreateRequest;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.service.WorkOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    public ApiResponse create(@RequestBody WorkOrderCreateRequest req,
                              @RequestParam Long creatorId) {
        try {
            WorkOrder wo = workOrderService.createWorkOrder(req, creatorId);
            return ApiResponse.ok(wo);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/today")
    public ApiResponse today(@RequestParam Long assigneeId) {
        List<WorkOrder> list = workOrderService.listTodayTasks(assigneeId);
        return ApiResponse.ok(list);
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id) {
        try {
            return ApiResponse.ok(workOrderService.getById(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/repair")
    public ApiResponse repair(@PathVariable Long id, @RequestBody RepairRequest req) {
        try {
            WorkOrder wo = workOrderService.submitRepair(id, req.getRecord());
            return ApiResponse.ok(wo);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}