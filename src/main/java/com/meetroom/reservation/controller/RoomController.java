package com.meetroom.reservation.controller;

import com.meetroom.reservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public String roomList(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("rooms", roomService.findActiveRooms(page));
        return "room/list";
    }
}
