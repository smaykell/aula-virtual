package io.github.smaykell.aulavirtual;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AulaVirtualApplication {

    public static void main(String[] args) {
        SpringApplication.run(AulaVirtualApplication.class, args);
    }
}
