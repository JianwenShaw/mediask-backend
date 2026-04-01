package me.jianwen.mediask.common.pagination;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record PageQuery(long pageNum, long pageSize) {

    public static final long DEFAULT_PAGE_NUM = 1L;
    public static final long DEFAULT_PAGE_SIZE = 20L;
    public static final long MAX_PAGE_NUM = 10_000L;
    public static final long MAX_PAGE_SIZE = 100L;

    public PageQuery {
        if (pageNum <= 0L) {
            throw new IllegalArgumentException("pageNum must be greater than 0");
        }
        if (pageNum > MAX_PAGE_NUM) {
            throw new IllegalArgumentException("pageNum must be less than or equal to " + MAX_PAGE_NUM);
        }
        if (pageSize <= 0L) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be less than or equal to " + MAX_PAGE_SIZE);
        }
    }

    public static PageQuery of(Long pageNum, Long pageSize) {
        return new PageQuery(
                pageNum == null ? DEFAULT_PAGE_NUM : ArgumentChecks.normalizePositive(pageNum, "pageNum"),
                pageSize == null ? DEFAULT_PAGE_SIZE : ArgumentChecks.normalizePositive(pageSize, "pageSize"));
    }
}
