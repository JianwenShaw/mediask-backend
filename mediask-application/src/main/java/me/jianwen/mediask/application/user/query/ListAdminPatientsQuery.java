package me.jianwen.mediask.application.user.query;

import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListAdminPatientsQuery(String keyword, PageQuery pageQuery) {

    public ListAdminPatientsQuery {
        keyword = ArgumentChecks.blankToNull(keyword);
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery must not be null");
        }
    }

    public static ListAdminPatientsQuery page(String keyword, Long pageNum, Long pageSize) {
        return new ListAdminPatientsQuery(keyword, PageQuery.of(pageNum, pageSize));
    }
}
