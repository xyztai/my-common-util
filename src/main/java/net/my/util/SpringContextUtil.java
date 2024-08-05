package net.my.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Data
@Slf4j
public class SpringContextUtil implements ApplicationContextAware {
    private static Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        environment = applicationContext.getEnvironment();
    }

    public static <T> T getProperty(String s, Class<T> clazz) {
        return environment.getProperty(s, clazz);
    }

    public static Boolean enableJwtFilter() {
        return SpringContextUtil.getProperty("jwt.filter", Boolean.class);
    }
}
