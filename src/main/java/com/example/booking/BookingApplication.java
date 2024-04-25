package com.example.booking;

import com.example.booking.entities.Role;
import com.example.booking.entities.Ticket;
import com.example.booking.entities.User;
import com.example.booking.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@SpringBootApplication
public class BookingApplication {

	private static final Logger log = LoggerFactory.getLogger(BookingApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BookingApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(TicketRepository repository) {
		return args -> {
			if(repository.count() == 0) {

				Set<Role> roles = Set.of(new Role(1L, Role.Values.ADMIN.name()));

				Ticket ticket = new Ticket(
						null,
						new User(null, "Ronaldo", "ronaldo123", null),
						"Show Gustavo Lima",
						"Vila Teixeira",
						LocalDateTime.of(2024, 4, 3, 16, 20),
						345.5
				);

				repository.save(ticket);
				log.info("Ingresso cadastrado com sucesso! " + ticket.getTicketName());
			}
		};
	}
}
