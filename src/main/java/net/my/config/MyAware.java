package net.my.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
public class MyAware implements ApplicationContextAware {

    @Value("${jwt.filter}")
    static String jwtFilter;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    public static boolean jwtFilter() {
        log.info("jwt.filter={}", jwtFilter);
        return Boolean.parseBoolean(jwtFilter);
    }
}
