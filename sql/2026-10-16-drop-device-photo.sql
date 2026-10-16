-- ============================================================================
-- 迁移脚本（2026-10-16）：移除「设备照片」功能
--
-- 背景：科室端设备照片实际价值低——维修人员必须到现场才能确认故障，
--       而医护人员报修时往往没时间拍照，该字段长期为空却额外占用报修流程与详情展示。
--       经确认整块移除：科室端上传、报修人端照片链接、管理端/维修端详情展示、接口字段与数据库列。
--
-- 说明：保留 /api/files/upload 与 uploads 目录 —— 签字验收与报销单签字仍依赖图片上传。
--
-- 适用：已建库的旧环境（全新库由 schema.sql 直接建出「无该列」的表结构）。
-- 幂等：可重复执行（有列才 DROP）。
--
-- 执行：mysql -uitsd -p it_service_desk < sql/2026-10-16-drop-device-photo.sql
-- ============================================================================

-- report.device_photo
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = 'device_photo');
SET @ddl1 := IF(@c1 > 0,
    'ALTER TABLE report DROP COLUMN device_photo',
    'DO 0');
PREPARE s1 FROM @ddl1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- 校验：应返回 0
-- SELECT COUNT(*) FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = 'device_photo';
