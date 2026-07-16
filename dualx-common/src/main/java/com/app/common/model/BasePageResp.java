package com.app.common.model;

import java.util.List;

public class BasePageResp<T> {

    private Integer total;

    private List<T> list;

    public BasePageResp(Integer total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public BasePageResp() {
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
