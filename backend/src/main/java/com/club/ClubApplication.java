package com.club;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 面向高校社团的活动组织与资源管理系统 - 启动类
 * 核心亮点：集成Claude LLM多Agent协同工作流，实现AI智能活动策划
 */
@SpringBootApplication
@MapperScan("com.club.mapper")
@EnableScheduling
public class ClubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClubApplication.class, args);
        System.out.println("========================================");
        System.out.println("  社团管理系统启动成功！");
        System.out.println("  Swagger: http://localhost:8080/doc.html");
            System.out.println("  前端:    http://localhost:5173");
        System.out.println("========================================");
    }
}
