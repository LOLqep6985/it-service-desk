INSERT IGNORE INTO sys_user (id, username, password, name, role) VALUES
(1, 'admin', 'admin123', '系统管理员', 'ADMIN'),
(2, 'dispatcher', '123456', '王调度', 'DISPATCHER'),
(3, 'repairer1', '123456', '张工', 'REPAIRER'),
(4, 'repairer2', '123456', '李工', 'REPAIRER'),
(5, 'reporter1', '123456', '王护士', 'REPORTER');

INSERT INTO department (id, name, campus, building, department_head) VALUES
(1, '门诊楼-三层', 'LAOYUAN', '门诊楼三层', '王护士长'),
(2, '住院部-五层', 'LAOYUAN', '住院部五层', '刘护士长'),
(3, '信息科机房', 'XIYUAN', '西院信息楼二层', '陈主任'),
(4, '门诊楼-二层', 'DONGYUAN', '东院门诊楼二层', '孙护士长'),
(5, '检验科', 'DONGYUAN', '东院检验楼一层', '李主任')
ON DUPLICATE KEY UPDATE department_head = VALUES(department_head);
INSERT IGNORE INTO department_binding (id, department_id, device_code, bind_at, active) VALUES
(1, 1, 'DEV001', NOW(), 1);
