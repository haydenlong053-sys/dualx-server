package com.app.common.model;

import java.util.Collections;

public class BaseSummaryPageResp<T> extends BasePageResp<T> {

    private T summary;

    public T getSummary() {
        return summary;
    }

    public void setSummary(T summary) {
        this.summary = summary;
    }

    public static BaseSummaryPageResp<?> emptyResult() {
        BaseSummaryPageResp<?> resp = new BaseSummaryPageResp<>();
        resp.setTotal(0);
        resp.setList(Collections.emptyList());
        return resp;
    }
}
