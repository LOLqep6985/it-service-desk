-- ============================================================
-- 迁移：报修单状态随工单流转同步（report.status 反映真实终态）
-- 日期：2026-10-15
-- 背景：
--   旧实现中 ReportStatus 有 ACCEPTED(已受理)，但 dispatch() 从未用过它，
--   派单直接 PENDING → DISPATCHED，之后 report.status 永远停在 DISPATCHED，
--   即使工单已签字完工（COMPLETED），报修历史列表仍显示「已派单」。
-- 本次改动：
--   1. ReportStatus 枚举：去掉 ACCEPTED，新增 COMPLETED
--   2. WorkOrderService.sign() 签字完工时回写 report.status = COMPLETED
--   3. 此脚本回填存量：把「对应工单已 COMPLETED」的 report.status 从 DISPATCHED 更新为 COMPLETED
-- 幂等：只在 work_order.status='COMPLETED' 且 report.status='DISPATCHED' 时更新；重复执行无副作用。
-- ============================================================

UPDATE report r
JOIN work_order w ON w.report_id = r.id
SET r.status = 'COMPLETED'
WHERE w.status = 'COMPLETED'
  AND r.status = 'DISPATCHED';

-- 说明：旧库若曾用 ACCEPTED 值（实测从未写入，这里防御性处理）
UPDATE report SET status = 'DISPATCHED' WHERE status = 'ACCEPTED';
