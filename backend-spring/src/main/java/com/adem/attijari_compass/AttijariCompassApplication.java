package com.adem.attijari_compass;

import com.adem.attijari_compass.config.IncomeMlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IncomeMlProperties.class)
public class AttijariCompassApplication {

	public static void main(String[] args) {
		SpringApplication.run(AttijariCompassApplication.class, args);
	}

}
