package vn.vanxuan.dtms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DtmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DtmsApplication.class, args);
    }
}
