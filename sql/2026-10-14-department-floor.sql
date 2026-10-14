-- ============================================================================
-- 迁移脚本：department 表新增 floor 列（楼层号，如 1F / 12F）
--
-- 背景：科室端绑定由「院区 → 科室」两级改为「院区 → 楼栋 → 科室」三级联动，
--       第三级需要在科室名前单独展示楼层号，因此把楼层作为**显式字段**建模，
--       而不是每次从 department.name（形如「门诊楼-一层-收费室」）里解析字符串。
--
-- 说明：name 保持「楼栋-中文楼层-科室名」不变——它承担全局唯一键（uk_dept_name）职责，
--       且 report.department_name / work_order.department_name 存的是名字快照。
--
-- 适用：已经建过库、department 表尚无 floor 列的旧环境。
--       全新库由 schema.sql 直接建出该列，无需执行本脚本。
--
-- 幂等：MySQL 8 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS，
--       因此用 INFORMATION_SCHEMA 判断后再动态执行 DDL，可重复运行。
--
-- 执行方式（任选其一）：
--   mysql -uitsd -p it_service_desk < sql/2026-10-14-department-floor.sql
--   或在 IDEA / Navicat 中打开本文件整体执行
--
-- 回填：本脚本只加列；floor 的取值由应用启动时的 data.sql 幂等回填（见该文件末尾）。
-- ============================================================================

-- 1) 缺列时才执行 ALTER
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'department'
      AND COLUMN_NAME = 'floor'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE department ADD COLUMN floor VARCHAR(20) NULL COMMENT ''楼层号，如 3F'' AFTER building',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 立即回填一次（与应用启动时的 data.sql 回填等价，便于跑完脚本就能看到结果）
UPDATE department
SET floor = CONCAT(CASE SUBSTRING_INDEX(SUBSTRING_INDEX(name, '-', 2), '-', -1)
                        WHEN '一层' THEN '1'
                        WHEN '二层' THEN '2'
                        WHEN '三层' THEN '3'
                        WHEN '四层' THEN '4'
                        WHEN '五层' THEN '5'
                        WHEN '六层' THEN '6'
                        WHEN '七层' THEN '7'
                        WHEN '八层' THEN '8'
                        WHEN '九层' THEN '9'
                        WHEN '十层' THEN '10'
                        WHEN '十一层' THEN '11'
                        WHEN '十二层' THEN '12'
                        ELSE NULL END, 'F')
WHERE (floor IS NULL OR floor = '')
  AND name LIKE '%-%-%';

-- 3) 校验：应返回 68 行、无 NULL floor
-- SELECT COUNT(*) AS total, COUNT(floor) AS with_floor FROM department;
