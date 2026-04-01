package me.jianwen.mediask.common.pagination;

import java.util.List;
import java.util.function.Function;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record PageData<T>(
        List<T> items,
        long pageNum,
        long pageSize,
        long total,
        long totalPages,
        boolean hasNext) implements PagedData<T> {

    public PageData {
        items = List.copyOf(items);
        if (pageNum <= 0L) {
            throw new IllegalArgumentException("pageNum must be greater than 0");
        }
        if (pageSize <= 0L) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (total < 0L) {
            throw new IllegalArgumentException("total must not be negative");
        }
        if (totalPages < 0L) {
            throw new IllegalArgumentException("totalPages must not be negative");
        }
    }

    public <R> PageData<R> map(Function<? super T, ? extends R> mapper) {
        ArgumentChecks.requireNonNull(mapper, "mapper");
        List<R> mappedItems = items.stream().map(mapper).map(value -> (R) value).toList();
        return new PageData<>(
                mappedItems,
                pageNum,
                pageSize,
                total,
                totalPages,
                hasNext);
    }
}
