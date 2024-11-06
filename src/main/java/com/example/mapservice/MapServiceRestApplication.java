package com.example.mapservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MapServiceRestApplication {

    public static void main(String[] args) {

        ApplicationContext applicationContext = SpringApplication.run(MapServiceRestApplication.class, args);

        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        for(String beanName : allBeanNames){
            System.out.println(beanName);
        }
    }

}
