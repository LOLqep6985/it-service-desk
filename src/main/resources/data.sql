-- 演示账号（密码为 BCrypt 哈希，明文见 README / 系统信息页；旧库明文会在登录成功后自动升级）
INSERT IGNORE INTO sys_user (id, username, password, name, role) VALUES
(1, 'admin', '$2a$10$StmZZTNgXMfPZL1w7YmeneHPBqGt6tD/4N9QgLyOtJqKB.eV.OQmu', '系统管理员', 'ADMIN'),
(2, 'dispatcher', '$2a$10$FezTSzbwDs5YUPOoEje.q.IYt1Q0WIIKZZ7oIrhohjACa9Wi1ByI6', '王调度', 'DISPATCHER'),
(3, 'repairer1', '$2a$10$94aUWwJK6oTZG72YbzZVyOtpHoqCIWDQsFnM0OvLIe8ILTmB7pqfu', '张工', 'REPAIRER'),
(4, 'repairer2', '$2a$10$BQ0vjtqeYfBeJaKmXsGPMOUXV3TjMzfObxhsDdgsQj8TLKNQcK3Nq', '李工', 'REPAIRER'),
(5, 'reporter1', '$2a$10$YkwhXmF1A/7y22fy9nHPe.YS0yTDBd.xpoEeIstDbe76WyicLPXQu', '王护士', 'REPORTER'),
(6, 'finance', '$2a$10$94aUWwJK6oTZG72YbzZVyOtpHoqCIWDQsFnM0OvLIe8ILTmB7pqfu', '李财务', 'FINANCE');

-- 科室索引（来源于：河南省人民医院豫东分院（民权县人民医院）楼层索引）
-- 命名规则：建筑-楼层-科室，name 唯一（uk_dept_name），INSERT IGNORE 幂等
INSERT IGNORE INTO department (name, campus, building) VALUES
-- 东区院区 · 门诊楼
('门诊楼-一层-收费室', 'DONGYUAN', '门诊楼'),
('门诊楼-一层-就诊服务中心', 'DONGYUAN', '门诊楼'),
('门诊楼-一层-药房', 'DONGYUAN', '门诊楼'),
('门诊楼-一层-CT', 'DONGYUAN', '门诊楼'),
('门诊楼-一层-磁共振', 'DONGYUAN', '门诊楼'),
('门诊楼-二层-颈肩腰腿疼科', 'DONGYUAN', '门诊楼'),
('门诊楼-二层-中医科', 'DONGYUAN', '门诊楼'),
('门诊楼-三层-五官科诊室', 'DONGYUAN', '门诊楼'),
('门诊楼-三层-口腔科诊室', 'DONGYUAN', '门诊楼'),
('门诊楼-四层-外科诊室', 'DONGYUAN', '门诊楼'),
('门诊楼-四层-骨科诊室', 'DONGYUAN', '门诊楼'),
('门诊楼-四层-心电图室', 'DONGYUAN', '门诊楼'),
('门诊楼-四层-彩超室', 'DONGYUAN', '门诊楼'),
('门诊楼-五层-检验科', 'DONGYUAN', '门诊楼'),
('门诊楼-五层-病理科', 'DONGYUAN', '门诊楼'),
('门诊楼-六层-健康管理中心', 'DONGYUAN', '门诊楼'),
('门诊楼-七层-骨科一病区', 'DONGYUAN', '门诊楼'),
('门诊楼-七层-关节外科', 'DONGYUAN', '门诊楼'),
('门诊楼-八层-骨科二病区', 'DONGYUAN', '门诊楼'),
('门诊楼-八层-脊柱外科', 'DONGYUAN', '门诊楼'),
-- 东区院区 · 病房楼
('病房楼-一层-静脉用药调配中心(PIVAS)', 'DONGYUAN', '病房楼'),
('病房楼-一层-化验室', 'DONGYUAN', '病房楼'),
('病房楼-一层-CT室', 'DONGYUAN', '病房楼'),
('病房楼-二层-五官科病区', 'DONGYUAN', '病房楼'),
('病房楼-二层-眼科', 'DONGYUAN', '病房楼'),
('病房楼-二层-耳鼻咽喉科', 'DONGYUAN', '病房楼'),
('病房楼-三层-产房', 'DONGYUAN', '病房楼'),
('病房楼-三层-新生儿重症监护病房(NICU)', 'DONGYUAN', '病房楼'),
('病房楼-四层-妇产科一病区', 'DONGYUAN', '病房楼'),
('病房楼-五层-妇产科二病区', 'DONGYUAN', '病房楼'),
('病房楼-六层-妇产科三病区', 'DONGYUAN', '病房楼'),
('病房楼-七层-普通外科一病区', 'DONGYUAN', '病房楼'),
('病房楼-八层-泌尿外科病区', 'DONGYUAN', '病房楼'),
('病房楼-九层-神经外科病区', 'DONGYUAN', '病房楼'),
('病房楼-十层-普通外科二病区', 'DONGYUAN', '病房楼'),
('病房楼-十层-胸外科', 'DONGYUAN', '病房楼'),
('病房楼-十一层-重症医学科(ICU)', 'DONGYUAN', '病房楼'),
('病房楼-十一层-手术室', 'DONGYUAN', '病房楼'),
('病房楼-十二层-手术室', 'DONGYUAN', '病房楼'),
-- 老院区 · 住院部1号楼
('住院部1号楼-一层-呼吸与危重症医学科', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-一层-呼吸重症监护病房(RICU)', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-一层-药房', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-一层-收费室', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-二层-消化内科病区', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-二层-老年病科病区', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-三层-肿瘤科一病区', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-三层-肿瘤科二病区', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-四层-内分泌科一病区', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-四层-血液透析室', 'LAOYUAN', '住院部1号楼'),
('住院部1号楼-五层-网络中心', 'LAOYUAN', '住院部1号楼'),
-- 老院区 · 急救中心
('急救中心-一层-EICU', 'LAOYUAN', '急救中心'),
('急救中心-一层-彩超室', 'LAOYUAN', '急救中心'),
('急救中心-一层-收费室', 'LAOYUAN', '急救中心'),
('急救中心-一层-药房', 'LAOYUAN', '急救中心'),
('急救中心-二层-急诊内科病房', 'LAOYUAN', '急救中心'),
('急救中心-二层-急诊留观病区', 'LAOYUAN', '急救中心'),
('急救中心-三层-急诊创伤外科病房', 'LAOYUAN', '急救中心'),
('急救中心-四层-120急救调度中心', 'LAOYUAN', '急救中心'),
('急救中心-四层-心电图室', 'LAOYUAN', '急救中心'),
('急救中心-四层-检验科', 'LAOYUAN', '急救中心'),
('急救中心-五层-法制办', 'LAOYUAN', '急救中心'),
('急救中心-五层-医疗纠纷调解委员会', 'LAOYUAN', '急救中心'),
-- 西院区 · 门诊
('门诊-一层-神经内科（一）', 'XIYUAN', '门诊'),
('门诊-二层-神经内科（二）', 'XIYUAN', '门诊'),
('门诊-三层-神经内科（三）', 'XIYUAN', '门诊'),
-- 西院区 · 住院部
('住院部-一层-心脑血管（一）', 'XIYUAN', '住院部'),
('住院部-二层-心脑血管（二）', 'XIYUAN', '住院部'),
('住院部-三层-心脑血管（三）', 'XIYUAN', '住院部');

-- ---------------------------------------------------------------------------
-- 幂等回填 department.floor（楼层号，如 3F）
-- 来源：科室 name 的中段为中文楼层（一层..十二层），在此显式落为规范楼层号，
--       供科室端「院区 → 楼栋 → 科室」三级联动在第三级展示楼层标签。
-- 说明：只回填空值，不覆盖已有数据；全新库与旧库（已执行 sql/2026-10-14-department-floor.sql）都适用。
-- ---------------------------------------------------------------------------
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
