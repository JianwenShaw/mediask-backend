package me.jianwen.mediask.application.user.query;

import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListAdminDoctorsQuery(String keyword, PageQuery pageQuery) {

    public ListAdminDoctorsQuery {
        keyword = ArgumentChecks.blankToNull(keyword);
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery must not be null");
        }
    }

    public static ListAdminDoctorsQuery page(String keyword, Long pageNum, Long pageSize) {
        return new ListAdminDoctorsQuery(keyword, PageQuery.of(pageNum, pageSize));
    }
}
