package com.club;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;
@SpringBootApplication @EnableScheduling @MapperScan("com.club.mapper")
public class QingjuApplication {
 public static void main(String[] args) { SpringApplication.run(QingjuApplication.class,args); }
}
