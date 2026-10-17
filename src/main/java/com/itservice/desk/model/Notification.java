package com.itservice.desk.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息。
 *
 * 投递维度两种：
 *  - USER   登录用户（管理员/调度/维修工），target_id = sys_user.id
 *  - DEVICE 科室端设备（免登录），target_id = department_binding.id
 *
 * 幂等键 (event_key, target_kind, target_id) 唯一：同一业务事件对同一收件人只产生一条，
 * 轮询重试/断线重连都不会重复弹、重复响。
 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventKey;
    private String targetKind;
    private Long targetId;
    private NotificationType type;
    private String title;
    private String body;
    /** NORMAL 只亮红点；URGENT 响铃 + 震动 */
    private String level;
    private Long workOrderId;
    private Long reportId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
