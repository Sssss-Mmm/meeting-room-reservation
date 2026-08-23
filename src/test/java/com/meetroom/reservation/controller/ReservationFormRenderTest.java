package com.meetroom.reservation.controller;

import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.domain.enums.Role;
import com.meetroom.reservation.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 폼 템플릿이 실제로 렌더링되는지 확인하는 스모크 테스트.
 * 날짜·시간 입력을 flatpickr에서 네이티브 datetime-local로 교체했기 때문에,
 * th:field 바인딩이 깨지면 여기서 바로 드러난다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationFormRenderTest {

    @Autowired
    private MockMvc mockMvc;

    // 레이아웃이 CustomUserDetails.getName()을 참조하므로 실제 principal 타입으로 로그인해야 한다
    private final CustomUserDetails principal = new CustomUserDetails(User.builder()
            .id(1L).username("user").password("pw")
            .name("사용자").email("user@test.com").role(Role.ROLE_USER)
            .build());

    @Test
    void 예약_폼은_네이티브_datetime_local_입력으로_렌더링된다() throws Exception {
        mockMvc.perform(get("/reservations/new").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"datetime-local\"")))
                .andExpect(content().string(containsString("id=\"startTime\"")))
                .andExpect(content().string(containsString("id=\"endTime\"")))
                .andExpect(content().string(not(containsString("flatpickr"))));
    }

    @Test
    void 빈_방_검색에서_넘어오면_회의실과_시간이_미리_채워진다() throws Exception {
        mockMvc.perform(get("/reservations/new").with(user(principal))
                        .param("roomId", "2")
                        .param("startTime", "2099-01-01T14:00")
                        .param("endTime", "2099-01-01T15:00")
                        .param("attendees", "6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2099-01-01T14:00")))
                .andExpect(content().string(containsString("value=\"2099-01-01T15:00")))
                .andExpect(content().string(containsString("value=\"6\"")));
    }

    @Test
    void 검증_실패로_폼을_다시_그릴_때_입력값이_datetime_local_형식으로_유지된다() throws Exception {
        mockMvc.perform(post("/reservations/new").with(user(principal)).with(csrf())
                        .param("roomId", "1")
                        .param("title", "")   // 제목 누락 -> 서버 검증 실패 -> 폼 재렌더링
                        .param("startTime", "2099-01-01T14:30")
                        .param("endTime", "2099-01-01T15:30")
                        .param("attendees", "2"))
                .andExpect(status().isOk())
                // ISO.DATE_TIME은 초까지 찍지만(...T14:30:00) datetime-local이 허용하는 형식이라 그대로 둔다
                .andExpect(content().string(containsString("value=\"2099-01-01T14:30")))
                .andExpect(content().string(containsString("value=\"2099-01-01T15:30")));
    }
}
