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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 빈 방 찾기 화면 스모크 테스트 — 카드 fragment와 예약 폼 프리필 링크가 실제로 렌더링되는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoomSearchRenderTest {

    @Autowired private MockMvc mockMvc;

    private final CustomUserDetails principal = new CustomUserDetails(User.builder()
            .id(1L).username("user").password("pw")
            .name("사용자").email("user@test.com").role(Role.ROLE_USER)
            .build());

    @Test
    void 검색하면_인원을_수용하는_방만_나오고_예약_링크에_시간이_채워진다() throws Exception {
        mockMvc.perform(get("/rooms").with(user(principal))
                        .param("start", "2099-01-01T14:00")
                        .param("end", "2099-01-01T15:00")
                        .param("attendees", "6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("소회의실B")))       // 정원 6
                .andExpect(content().string(not(containsString("휴게실겸 회의실")))) // 정원 4 — 제외
                .andExpect(content().string(containsString("startTime=2099-01-01T14:00")))
                .andExpect(content().string(containsString("attendees=6")));
    }

    @Test
    void 검색하지_않으면_전체_목록이_나오고_예약_링크에_빈_시간이_붙지_않는다() throws Exception {
        mockMvc.perform(get("/rooms").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("휴게실겸 회의실")))
                .andExpect(content().string(not(containsString("startTime="))));
    }

    @Test
    void 종료가_시작보다_빠르면_목록으로_돌려보낸다() throws Exception {
        mockMvc.perform(get("/rooms").with(user(principal))
                        .param("start", "2099-01-01T15:00")
                        .param("end", "2099-01-01T14:00"))
                .andExpect(status().is3xxRedirection());
    }
}
