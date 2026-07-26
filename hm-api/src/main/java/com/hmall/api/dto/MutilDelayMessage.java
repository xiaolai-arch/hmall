package com.hmall.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class MutilDelayMessage<T> {
    private T data;
    private List<Long> times;

    /**
     * 时间list里面有没有时间
     * */
    public boolean hasNext() {
        return times != null && !times.isEmpty();
    }

    public Long popNextTime() {
        return times.remove(0);
    }

}
