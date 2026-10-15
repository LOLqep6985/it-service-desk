-- ============================================================================
-- 迁移脚本（2026-10-15）：新增「财务」角色演示账号
--
-- 背景：财务是独立岗位（只能查看/处理报销单，不接触工单与系统配置），
--       权限矩阵见 findings.md#D-011 / progress.md#S-015。
-- 幂等：INSERT IGNORE（按 username 唯一）；已是新库则由 data.sql 直接建出。
-- 说明：密码哈希与 repairer1 相同（演示明文均为 123456，BCrypt 哈希与用户无关）。
--
-- 执行：mysql -uitsd -p it_service_desk < sql/2026-10-15-finance-role.sql
-- ============================================================================

INSERT IGNORE INTO sys_user (id, username, password, name, role) VALUES
(6, 'finance', '$2a$10$94aUWwJK6oTZG72YbzZVyOtpHoqCIWDQsFnM0OvLIe8ILTmB7pqfu', '李财务', 'FINANCE');

-- 校验
-- SELECT id, username, name, role FROM sys_user ORDER BY id;
