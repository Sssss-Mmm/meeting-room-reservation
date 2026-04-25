package com.everspin.reservation.config;

import com.everspin.reservation.domain.Room;
import com.everspin.reservation.domain.User;
import com.everspin.reservation.domain.enums.Role;
import com.everspin.reservation.repository.RoomRepository;
import com.everspin.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitDataConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .name("관리자")
                    .email("admin@everspin.com")
                    .department("경영지원팀")
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
        }

        if (roomRepository.count() == 0) {
            String[][] rooms = {
                {"대회의실A", "1층 로비 옆", "20", "프로젝터 및 화상회의 장비 구비"},
                {"소회의실B", "2층 201호", "6", "화이트보드 구비"},
                {"회의실C", "2층 202호", "8", "TV 모니터 구비"},
                {"임원회의실", "3층", "10", "VIP용 회의실"},
                {"휴게실겸 회의실", "지하 1층", "4", "캐주얼한 미팅용"}
            };

            for (String[] r : rooms) {
                Room room = Room.builder()
                        .name(r[0])
                        .location(r[1])
                        .capacity(Integer.parseInt(r[2]))
                        .description(r[3])
                        .build();
                roomRepository.save(room);
            }
        }
    }
}
