package com.itservice.desk.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件配置（待办三：列表分页）。
 *
 * <p>修复前：所有列表接口全量返回（{@code selectList} 无上限），
 * 数据量增长后既耗数据库也耗前端内存。
 *
 * <p>注意：{@link PaginationInnerInterceptor} 必须显式注册，
 * 否则 {@code Page} 参数会被静默忽略并退化成全量查询——
 * 这是一个很容易踩的坑（不报错，但分页"看起来没生效"）。
 */
@Configuration
public class MybatisPlusConfig {

    /** 单页最大条数：防止调用方传 size=999999 变相拉全量 */
    private static final long MAX_LIMIT = 200L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_LIMIT);
        // 请求页码超过总页数时返回空列表，而不是回到第一页（避免前端误以为有数据）
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
