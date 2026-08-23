package com.meetroom.reservation.controller;

import com.meetroom.reservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public String roomList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                           @RequestParam(defaultValue = "1") int attendees,
                           Model model) {
        model.addAttribute("rooms", roomService.findActiveRooms(page));
        if (start != null && end != null) {
            // Thymeleaf 3.1은 fragment 표현식 안에서 param 접근을 막는다 — 검색 조건을 모델로 넘긴다
            model.addAttribute("availableRooms", roomService.findAvailableRooms(start, end, attendees));
            model.addAttribute("searchStart", start);
            model.addAttribute("searchEnd", end);
            model.addAttribute("searchAttendees", attendees);
        }
        return "room/list";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleRejectedRequest(RuntimeException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/rooms";
    }
}
