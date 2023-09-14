package com.madre.excelweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExcelwebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelwebApplication.class, args);
    }

}
