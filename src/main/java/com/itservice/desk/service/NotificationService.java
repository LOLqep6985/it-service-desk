package com.itservice.desk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itservice.desk.mapper.DepartmentBindingMapper;
import com.itservice.desk.mapper.DepartmentMapper;
import com.itservice.desk.mapper.NotificationMapper;
import com.itservice.desk.mapper.ReportMapper;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.DepartmentBinding;
import com.itservice.desk.model.Notification;
import com.itservice.desk.model.NotificationType;
import com.itservice.desk.model.Report;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息中心（A 档）。
 *
 * 设计要点：
 *  - 服务端是唯一事实来源，客户端只做「拉取 + 红点 + 响铃」，断线/切后台后按 since 补齐即可，不丢消息。
 *  - 事件只做 4 类（D-030）：报修提交、派单、待签字、超时未接单。
 *  - 不做已读回执给管理员：read_at 仅供收件人自己消红点。
 */
@Service
public class NotificationService {

    public static final String KIND_USER = "USER";
    public static final String KIND_DEVICE = "DEVICE";
    public static final String LEVEL_NORMAL = "NORMAL";
    public static final String LEVEL_URGENT = "URGENT";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final DepartmentBindingMapper bindingMapper;
    private final DepartmentMapper departmentMapper;
    private final ReportMapper reportMapper;

    public NotificationService(NotificationMapper notificationMapper,
                               UserMapper userMapper,
                               DepartmentBindingMapper bindingMapper,
                               DepartmentMapper departmentMapper,
                               ReportMapper reportMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.departmentMapper = departmentMapper;
        this.reportMapper = reportMapper;
    }

    // ---------- 写入 ----------

    public void notifyUser(Long userId, NotificationType type, String eventKey, String title, String body,
                           String level, Long workOrderId, Long reportId) {
        if (userId == null) {
            return;
        }
        insert(KIND_USER, userId, type, eventKey, title, body, level, workOrderId, reportId);
    }

    public void notifyDevice(Long bindingId, NotificationType type, String eventKey, String title, String body,
                             String level, Long workOrderId, Long reportId) {
        if (bindingId == null) {
            return;
        }
        insert(KIND_DEVICE, bindingId, type, eventKey, title, body, level, workOrderId, reportId);
    }

    /** 给所有管理员/调度员各发一条 */
    public void notifyAdmins(NotificationType type, String eventKey, String title, String body,
                             String level, Long workOrderId, Long reportId) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRole, UserRole.ADMIN, UserRole.DISPATCHER));
        for (User u : admins) {
            notifyUser(u.getId(), type, eventKey, title, body, level, workOrderId, reportId);
        }
    }

    private void insert(String kind, Long targetId, NotificationType type, String eventKey, String title,
                        String body, String level, Long workOrderId, Long reportId) {
        Notification n = new Notification();
        n.setEventKey(eventKey);
        n.setTargetKind(kind);
        n.setTargetId(targetId);
        n.setType(type);
        n.setTitle(truncate(title, 200));
        n.setBody(truncate(body, 500));
        n.setLevel(level == null ? LEVEL_NORMAL : level);
        n.setWorkOrderId(workOrderId);
        n.setReportId(reportId);
        n.setCreatedAt(LocalDateTime.now());
        try {
            notificationMapper.insertIgnore(n);
        } catch (Exception ignored) {
            // 通知是旁路：任何异常都不能影响主流程（派单/签字/报修）
        }
    }

    // ---------- 业务事件 ----------

    /** 事件4：维修完成待签字 → 报修人设备 + 护士长设备（同名时只发护士长设备） */
    public void notifyPendingSign(WorkOrder wo) {
        if (wo == null) {
            return;
        }
        try {
            Report report = wo.getReportId() == null ? null : reportMapper.selectById(wo.getReportId());
            Long deptId = wo.getDepartmentId() != null ? wo.getDepartmentId()
                    : (report == null ? null : report.getDepartmentId());
            if (deptId == null) {
                return;
            }

            List<DepartmentBinding> heads = bindingMapper.selectList(new LambdaQueryWrapper<DepartmentBinding>()
                    .eq(DepartmentBinding::getDepartmentId, deptId)
                    .eq(DepartmentBinding::getIsHead, true)
                    .eq(DepartmentBinding::getActive, true));

            List<DepartmentBinding> submitters = new ArrayList<>();
            if (report != null && report.getClientDeviceId() != null && !report.getClientDeviceId().isBlank()) {
                submitters.addAll(bindingMapper.selectList(new LambdaQueryWrapper<DepartmentBinding>()
                        .eq(DepartmentBinding::getDeviceCode, report.getClientDeviceId())
                        .eq(DepartmentBinding::getActive, true)));
            }

            // 报修人 == 护士长（同人）→ 只发护士长设备，避免本人手机上收到两条
            boolean samePerson = report != null && sameName(report.getReporterName(), headNameOf(deptId));

            List<DepartmentBinding> targets = new ArrayList<>();
            if (samePerson) {
                targets.addAll(heads);
            } else {
                targets.addAll(submitters);
                targets.addAll(heads);
            }
            if (targets.isEmpty()) {
                return;
            }

            String title = "工单待签字：" + safe(wo.getTitle());
            String body = safe(wo.getDepartmentName()) + " · " + safe(report == null ? null : report.getReporterName())
                    + " 报修，维修已完成，请签字确认";
            String eventKey = "PENDING_SIGN:" + wo.getId();

            List<Long> sent = new ArrayList<>();
            for (DepartmentBinding b : targets) {
                if (b == null || b.getId() == null || sent.contains(b.getId())) {
                    continue;
                }
                sent.add(b.getId());
                notifyDevice(b.getId(), NotificationType.PENDING_SIGN, eventKey, title, body, LEVEL_URGENT,
                        wo.getId(), report == null ? null : report.getId());
            }
        } catch (Exception ignored) {
            // 旁路
        }
    }

    // ---------- 读取 ----------

    public Map<String, Object> listFor(String kind, Long targetId, long since, int limit) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (targetId == null) {
            data.put("items", List.of());
            data.put("unread", 0);
            data.put("maxId", since);
            return data;
        }
        int size = Math.max(1, Math.min(limit, 50));
        List<Notification> items = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetKind, kind)
                .eq(Notification::getTargetId, targetId)
                .gt(Notification::getId, since)
                .orderByAsc(Notification::getId)
                .last("LIMIT " + size));
        long unread = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetKind, kind)
                .eq(Notification::getTargetId, targetId)
                .isNull(Notification::getReadAt));
        long maxId = since;
        for (Notification n : items) {
            if (n.getId() != null && n.getId() > maxId) {
                maxId = n.getId();
            }
        }
        data.put("items", items);
        data.put("unread", unread);
        data.put("maxId", maxId);
        return data;
    }

    /** 标记已读（只影响该收件人自己的红点，不对外暴露回执） */
    public int markRead(String kind, Long targetId, List<Long> ids) {
        if (targetId == null || ids == null || ids.isEmpty()) {
            return 0;
        }
        return notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getTargetKind, kind)
                .eq(Notification::getTargetId, targetId)
                .in(Notification::getId, ids)
                .isNull(Notification::getReadAt)
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    public int markAllRead(String kind, Long targetId) {
        if (targetId == null) {
            return 0;
        }
        return notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getTargetKind, kind)
                .eq(Notification::getTargetId, targetId)
                .isNull(Notification::getReadAt)
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    // ---------- 工具 ----------

    private String headNameOf(Long deptId) {
        Department d = departmentMapper.selectById(deptId);
        return d == null ? null : d.getDepartmentHead();
    }

    /** 姓名比对：去空白、全角空格，规避手工输入差异 */
    private boolean sameName(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = a.replaceAll("[\\s\\u3000]", "");
        String y = b.replaceAll("[\\s\\u3000]", "");
        return !x.isEmpty() && x.equals(y);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
