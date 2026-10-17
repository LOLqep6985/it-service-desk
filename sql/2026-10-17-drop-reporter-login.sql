-- 2026-10-17 移除「登录式报修」通道（D-033）
-- 科室报修统一走免登录入口 report.html；报修人不再需要登录账号。
-- 说明：一次性脚本。若该用户已被手工删除，DELETE 无副作用。

DELETE FROM sys_user WHERE username = 'reporter1';
