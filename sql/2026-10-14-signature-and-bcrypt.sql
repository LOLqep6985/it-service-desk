-- ============================================================================
-- 迁移脚本（2026-10-14）：报销单签字留痕 + 密码 BCrypt 化
--
-- 背景：修复 findings.md#D-003 中记录的已知限制：
--   1) 报销单签字只记录签字人/时间，手写签名图片未落库 → 新增 signature_url
--   2) 演示账号密码明文存储 → 改为 BCrypt 哈希（$2a$10$...）
--
-- 适用：已经建过库的旧环境。全新库由 schema.sql + data.sql 直接建出正确状态。
-- 幂等：可重复执行（缺列才 ALTER；仅当密码仍是明文时才 UPDATE）。
--
-- 执行：mysql -uitsd -p it_service_desk < sql/2026-10-14-signature-and-bcrypt.sql
-- ============================================================================

-- 1) reimbursement 增加 signature_url（科室签字的手写签名图片）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reimbursement' AND COLUMN_NAME = 'signature_url'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE reimbursement ADD COLUMN signature_url VARCHAR(255) NULL COMMENT ''科室签字手写签名图片'' AFTER reimbursed_at',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 演示账号密码 BCrypt 化（仅当仍是明文时更新，避免覆盖已改过的密码）
UPDATE sys_user SET password = '$2a$10$StmZZTNgXMfPZL1w7YmeneHPBqGt6tD/4N9QgLyOtJqKB.eV.OQmu' WHERE username = 'admin'      AND password = 'admin123';
UPDATE sys_user SET password = '$2a$10$FezTSzbwDs5YUPOoEje.q.IYt1Q0WIIKZZ7oIrhohjACa9Wi1ByI6' WHERE username = 'dispatcher' AND password = '123456';
UPDATE sys_user SET password = '$2a$10$94aUWwJK6oTZG72YbzZVyOtpHoqCIWDQsFnM0OvLIe8ILTmB7pqfu' WHERE username = 'repairer1'  AND password = '123456';
UPDATE sys_user SET password = '$2a$10$BQ0vjtqeYfBeJaKmXsGPMOUXV3TjMzfObxhsDdgsQj8TLKNQcK3Nq' WHERE username = 'repairer2'  AND password = '123456';
UPDATE sys_user SET password = '$2a$10$YkwhXmF1A/7y22fy9nHPe.YS0yTDBd.xpoEeIstDbe76WyicLPXQu' WHERE username = 'reporter1'  AND password = '123456';

-- 3) 校验：password 应全部以 $2a$ 开头
-- SELECT username, LEFT(password, 7) AS prefix FROM sys_user;
