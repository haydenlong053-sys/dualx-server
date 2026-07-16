package com.app.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BaseComponent {

    @Value(value = "${spring.profiles.active}")
    protected String env;

    protected static final byte ONE = 1;
    protected static final byte ZERO = 0;
}
