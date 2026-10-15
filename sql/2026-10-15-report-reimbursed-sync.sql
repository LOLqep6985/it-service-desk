-- ============================================================
-- 迁移：报销单财务回填后同步报修单为「已报销」（report.status = REIMBURSED）
-- 日期：2026-10-15
-- 背景：
--   报修单状态机新增 REIMBURSED(已报销)。财务回填（reimbursement.status=REIMBURSED）后，
--   报销单内工单对应的报修单应回写为 REIMBURSED，使报修历史反映整条链路闭环。
--   代码侧由 ReimbursementService.reimburse() 同步回写；此脚本回填存量。
-- 幂等：只更新「报修单为 COMPLETED 且对应工单已在已报销报销单内」的行；重复执行无副作用。
-- ============================================================

UPDATE report r
JOIN work_order w ON w.report_id = r.id
JOIN reimbursement_item ri ON ri.work_order_id = w.id
JOIN reimbursement rb ON rb.id = ri.reimbursement_id
SET r.status = 'REIMBURSED'
WHERE rb.status = 'REIMBURSED'
  AND r.status = 'COMPLETED';
