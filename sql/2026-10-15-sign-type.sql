-- ============================================================================
-- 迁移脚本（2026-10-15）：签字方式留痕 sign_type
--
-- 背景：科室工作机多为无触屏 PC，鼠标手写签名体验差；
--       新增「输入姓名生成签名」便捷模式，并**记录签字方式**以便审计追溯：
--         HANDWRITTEN = 手写签名（默认，医院正式场景）
--         TYPED       = 输入姓名生成（便捷模式，界面明确标注）
--
-- 适用：已建库的旧环境（全新库由 schema.sql 直接建出该列）。
-- 幂等：可重复执行（缺列才 ALTER）；已有历史数据保持 NULL（界面显示"未记录"）。
--
-- 执行：mysql -uitsd -p it_service_desk < sql/2026-10-15-sign-type.sql
-- ============================================================================

-- work_order.sign_type
SET @c1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'work_order' AND COLUMN_NAME = 'sign_type');
SET @ddl1 := IF(@c1 = 0,
    'ALTER TABLE work_order ADD COLUMN sign_type VARCHAR(20) NULL COMMENT ''签字方式：HANDWRITTEN/TYPED'' AFTER signature_url',
    'DO 0');
PREPARE s1 FROM @ddl1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- reimbursement.sign_type
SET @c2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reimbursement' AND COLUMN_NAME = 'sign_type');
SET @ddl2 := IF(@c2 = 0,
    'ALTER TABLE reimbursement ADD COLUMN sign_type VARCHAR(20) NULL COMMENT ''签字方式：HANDWRITTEN/TYPED'' AFTER signature_url',
    'DO 0');
PREPARE s2 FROM @ddl2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 校验：应各返回 1
-- SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'sign_type';
