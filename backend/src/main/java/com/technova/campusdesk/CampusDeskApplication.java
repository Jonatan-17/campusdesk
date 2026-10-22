package com.technova.campusdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend. @SpringBootApplication activa la configuración automática de Spring Boot
 * y el escaneo de componentes: todas las clases con @Service, @RestController, @Component, etc.
 * dentro de com.technova.campusdesk se registran como beans.
 */
@SpringBootApplication
public class CampusDeskApplication {

	public static void main(String[] args) {
		// Arranca el contexto de Spring y el servidor Tomcat embebido (puerto SERVER_PORT, 8080 por defecto)
		SpringApplication.run(CampusDeskApplication.class, args);
	}

}
