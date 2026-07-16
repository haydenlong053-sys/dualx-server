package com.app.common.model;

import lombok.Data;

@Data
public class EnumModel {

    private String key;

    private String label;

    public EnumModel() {
    }

    public EnumModel(String key, String label) {
        this.key = key;
        this.label = label;
    }
}
