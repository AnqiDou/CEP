package cep_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CepBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CepBackendApplication.class, args);
	}

}
