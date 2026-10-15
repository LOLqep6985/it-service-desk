-- ============================================================
-- 迁移：工单状态机重构（严格对齐《工单全流程规则.md》6 态）
-- 日期：2026-10-15
-- 背景：
--   旧 WorkOrderStatus: PENDING(未用)/ASSIGNED/IN_PROGRESS/PENDING_ACCEPT/COMPLETED
--   新 WorkOrderStatus: ASSIGNED/IN_PROGRESS/PENDING_PRICING/PENDING_SIGN/PENDING_REIMBURSE/REIMBURSED
--   流转: 派单→待接单 → 接单→维修中 → 提交关闭→待核价 → 核价→待签字 → 签字→待报销 → 月度报销→已报销
-- 映射规则（存量工单）：
--   PENDING_ACCEPT(待签字,未签字) → PENDING_SIGN(待签字)        [护士长还没签字]
--   COMPLETED + UNSIGNED          → PENDING_PRICING(待核价)        [异常数据：已完工却未签字，回退到待核价]
--   COMPLETED + SIGNED 且未在已报销报销单内 → PENDING_REIMBURSE(待报销)  [已签字验收，等待月度报销]
--   COMPLETED + SIGNED 且在已报销报销单内   → REIMBURSED(已报销)          [整条链路已闭环]
--   PENDING(旧待受理,从未使用) → ASSIGNED 兜底（防御，实际无此数据）
-- 幂等：只在 status 还是旧值时更新；重复执行无副作用。
-- ============================================================

-- 1. PENDING_ACCEPT → PENDING_SIGN（待签字未变，只是枚举名变）
UPDATE work_order SET status = 'PENDING_SIGN' WHERE status = 'PENDING_ACCEPT';

-- 2. COMPLETED + SIGNED 且在已报销报销单内 → REIMBURSED
UPDATE work_order w
JOIN reimbursement_item ri ON ri.work_order_id = w.id
JOIN reimbursement r ON r.id = ri.reimbursement_id
SET w.status = 'REIMBURSED'
WHERE w.status = 'COMPLETED'
  AND w.sign_status = 'SIGNED'
  AND r.status = 'REIMBURSED';

-- 3. COMPLETED + SIGNED 且未在已报销报销单内 → PENDING_REIMBURSE（待报销）
UPDATE work_order SET status = 'PENDING_REIMBURSE'
WHERE status = 'COMPLETED'
  AND sign_status = 'SIGNED';

-- 4. COMPLETED + UNSIGNED（异常）→ PENDING_PRICING（回退到待核价，等管理员核价）
UPDATE work_order SET status = 'PENDING_PRICING'
WHERE status = 'COMPLETED'
  AND (sign_status = 'UNSIGNED' OR sign_status IS NULL);

-- 5. 旧 PENDING（从未使用，防御性兜底）→ ASSIGNED
UPDATE work_order SET status = 'ASSIGNED' WHERE status = 'PENDING';

-- 说明：ASSIGNED / IN_PROGRESS 旧值与新值含义一致，无需迁移。
