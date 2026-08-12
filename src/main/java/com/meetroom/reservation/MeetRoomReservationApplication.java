package com.meetroom.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MeetRoomReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeetRoomReservationApplication.class, args);
	}

}
