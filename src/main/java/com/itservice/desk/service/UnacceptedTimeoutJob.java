package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itservice.desk.mapper.WorkOrderMapper;
import com.itservice.desk.model.NotificationType;
import com.itservice.desk.model.WorkOrder;
import com.itservice.desk.model.WorkOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件9：超时未接单提醒。
 *
 * 每分钟扫一次「已派单(ASSIGNED) 且派单超过 15 分钟仍未接单」的工单，
 * 给维修工发 URGENT、给管理员/调度发 NORMAL。
 *
 * 幂等：消息表 (event_key, target_kind, target_id) 唯一 + INSERT IGNORE，
 * 所以同一工单对同一人只会提醒一次，扫描多少次都不会重复轰炸。
 */
@Component
public class UnacceptedTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(UnacceptedTimeoutJob.class);

    /** 派单后多少分钟仍未接单算超时 */
    public static final int TIMEOUT_MINUTES = 15;

    private final WorkOrderMapper workOrderMapper;
    private final NotificationService notificationService;

    public UnacceptedTimeoutJob(WorkOrderMapper workOrderMapper, NotificationService notificationService) {
        this.workOrderMapper = workOrderMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scan() {
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
            List<WorkOrder> overdue = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getStatus, WorkOrderStatus.ASSIGNED)
                    .le(WorkOrder::getCreatedAt, deadline));

            for (WorkOrder wo : overdue) {
                String key = "TIMEOUT:" + wo.getId();
                String title = "工单超时未接单：" + (wo.getTitle() == null ? "" : wo.getTitle());
                String body = "派单已超过 " + TIMEOUT_MINUTES + " 分钟仍未接单，请尽快处理";
                notificationService.notifyUser(wo.getAssigneeId(), NotificationType.TIMEOUT_UNACCEPTED,
                        key, title, body, NotificationService.LEVEL_URGENT, wo.getId(), wo.getReportId());
                notificationService.notifyAdmins(NotificationType.TIMEOUT_UNACCEPTED,
                        key, title, body, NotificationService.LEVEL_NORMAL, wo.getId(), wo.getReportId());
            }
        } catch (Exception e) {
            log.warn("超时未接单扫描失败（不影响主流程）：{}", e.getMessage());
        }
    }
}
