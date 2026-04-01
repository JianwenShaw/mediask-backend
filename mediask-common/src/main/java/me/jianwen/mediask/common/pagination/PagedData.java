package me.jianwen.mediask.common.pagination;

// TODO: 后续考虑支持游标分页，当前实现暂不涉及
public sealed interface PagedData<T> permits PageData{}
