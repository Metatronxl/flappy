package com.noisy.flappy.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author lei.X
 * @date 2019/6/28
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class})
public class ProxyServerApplication {


    public static void main(String[] args) {

//        SpringApplication.run(ProxyServerApplication.class, args);
        ConfigurableApplicationContext context = SpringApplication.run(ProxyServerApplication.class, args);
        ProxyServerContainer bean = context.getBean(ProxyServerContainer.class);
        bean.start();

    }
}
