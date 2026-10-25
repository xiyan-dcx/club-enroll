package com.club.util;
import org.springframework.context.*;
import org.springframework.stereotype.Component;
@Component public class Beans implements ApplicationContextAware {
 private static ApplicationContext context;
 public void setApplicationContext(ApplicationContext value){context=value;}
 public static <T> T get(Class<T> type){return context.getBean(type);}
}
