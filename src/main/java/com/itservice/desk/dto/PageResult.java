package com.itservice.desk.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应体（待办三：列表分页）。
 *
 * <h2>为什么要有这个类，而不是直接返回 MyBatis-Plus 的 Page</h2>
 * {@code Page} 内部字段多（orders、optimizeCountSql、searchCount...），
 * 直接序列化会把实现细节暴露给前端，接口契约不稳定。这里只暴露必要字段。
 *
 * <h2>与前端既有契约的兼容策略（重要）</h2>
 * 既有前端多处把返回值直接当数组用（{@code state.rows.filter(...)}）。
 * 若强行改成对象会全部打挂。因此接口设计为：
 * <ul>
 *   <li><b>不传 page 参数</b> → 仍返回<b>数组</b>（老行为，完全兼容）；</li>
 *   <li><b>传了 page 参数</b> → 返回本分页对象（新能力，按需启用）。</li>
 * </ul>
 * 这样前端可以逐页迁移，不会一次性全崩。
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;
    /** 总条数（前端据此算总页数） */
    private long total;
    /** 当前页码（从 1 开始） */
    private long page;
    /** 每页条数 */
    private long size;
    /** 总页数 */
    private long pages;

    public PageResult() {
    }

    public PageResult(IPage<T> p) {
        this.records = p.getRecords();
        this.total = p.getTotal();
        this.page = p.getCurrent();
        this.size = p.getSize();
        this.pages = p.getPages();
    }
}
