package me.jianwen.mediask.application.user.query;

import me.jianwen.mediask.common.pagination.PageQuery;

public record ListAdminDepartmentsQuery(String keyword, PageQuery pageQuery) {

    public static ListAdminDepartmentsQuery page(String keyword, Long pageNum, Long pageSize) {
        return new ListAdminDepartmentsQuery(keyword, PageQuery.of(pageNum, pageSize));
    }
}
