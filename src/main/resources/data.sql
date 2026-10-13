INSERT IGNORE INTO sys_user (id, username, password, name, role) VALUES
(1, 'admin', 'admin123', '系统管理员', 'ADMIN'),
(2, 'dispatcher', '123456', '王调度', 'DISPATCHER'),
(3, 'repairer1', '123456', '张工', 'REPAIRER'),
(4, 'repairer2', '123456', '李工', 'REPAIRER'),
(5, 'reporter1', '123456', '王护士', 'REPORTER');

INSERT IGNORE INTO department (id, name, campus, building) VALUES
(1, '门诊楼-三层', 'LAOYUAN', '门诊楼三层'),
(2, '住院部-五层', 'LAOYUAN', '住院部五层'),
(3, '信息科机房', 'XIYUAN', '西院信息楼二层'),
(4, '门诊楼-二层', 'DONGYUAN', '东院门诊楼二层'),
(5, '检验科', 'DONGYUAN', '东院检验楼一层');