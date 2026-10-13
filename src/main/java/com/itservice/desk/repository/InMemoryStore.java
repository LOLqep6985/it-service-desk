package com.itservice.desk.repository;

import com.itservice.desk.model.Campus;
import com.itservice.desk.model.Department;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import com.itservice.desk.model.WorkOrder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStore {

    public final Map<Long, User> users = new ConcurrentHashMap<>();
    public final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    public final Map<Long, Department> departments = new ConcurrentHashMap<>();
    public final Map<Long, WorkOrder> workOrders = new ConcurrentHashMap<>();
    public final AtomicLong userIdSeq = new AtomicLong(1);
    public final AtomicLong deptIdSeq = new AtomicLong(1);
    public final AtomicLong orderIdSeq = new AtomicLong(1);

    @PostConstruct
    public void init() {
        addUser("admin", "admin123", "系统管理员", UserRole.ADMIN);
        addUser("dispatcher", "123456", "王调度", UserRole.DISPATCHER);
        addUser("repairer1", "123456", "张工", UserRole.REPAIRER);
        addUser("repairer2", "123456", "李工", UserRole.REPAIRER);
        addUser("reporter1", "123456", "王护士", UserRole.REPORTER);

        addDepartment("门诊楼-三层", Campus.LAOYUAN, "门诊楼三层");
        addDepartment("住院部-五层", Campus.LAOYUAN, "住院部五层");
        addDepartment("信息科机房", Campus.XIYUAN, "西院信息楼二层");
        addDepartment("门诊楼-二层", Campus.DONGYUAN, "东院门诊楼二层");
        addDepartment("检验科", Campus.DONGYUAN, "东院检验楼一层");
    }

    private void addUser(String username, String password, String name, UserRole role) {
        User u = new User();
        u.setId(userIdSeq.getAndIncrement());
        u.setUsername(username);
        u.setPassword(password);
        u.setName(name);
        u.setRole(role);
        users.put(u.getId(), u);
        usersByUsername.put(username, u);
    }

    private void addDepartment(String name, Campus campus, String building) {
        Department d = new Department();
        d.setId(deptIdSeq.getAndIncrement());
        d.setName(name);
        d.setCampus(campus);
        d.setBuilding(building);
        departments.put(d.getId(), d);
    }
}