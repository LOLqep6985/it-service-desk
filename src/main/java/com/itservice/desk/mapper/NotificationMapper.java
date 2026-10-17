package com.itservice.desk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itservice.desk.model.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * INSERT IGNORE：靠 (event_key, target_kind, target_id) 唯一索引做幂等。
     * 同一事件对同一收件人只会落一条，重复触发（轮询/重试/定时任务重复扫描）不会产生重复消息。
     */
    @Insert("INSERT IGNORE INTO notification"
            + " (event_key, target_kind, target_id, type, title, body, level, work_order_id, report_id, created_at)"
            + " VALUES (#{n.eventKey}, #{n.targetKind}, #{n.targetId}, #{n.type}, #{n.title}, #{n.body},"
            + " #{n.level}, #{n.workOrderId}, #{n.reportId}, #{n.createdAt})")
    int insertIgnore(@Param("n") Notification n);
}
