package com.meetroom.reservation.controller;

import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.service.ReservationService;
import com.meetroom.reservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReservationService reservationService;
    private final RoomService roomService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int pendingPage, Model model) {
        model.addAttribute("pendingReservations", reservationService.findPendingReservations(pendingPage));
        model.addAttribute("todayReservations", reservationService.findTodayReservations());
        return "admin/dashboard";
    }

    @PostMapping("/reservations/{id}/approve")
    public String approveReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.approveReservation(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/reservations/{id}/reject")
    public String rejectReservation(@PathVariable Long id, @RequestParam String reason,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.rejectReservation(id, reason);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/rooms")
    public String manageRooms(Model model) {
        model.addAttribute("rooms", roomService.findAllRooms());
        return "admin/rooms";
    }

    @PostMapping("/rooms")
    public String createRoom(@ModelAttribute Room room) {
        roomService.createRoom(room);
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/{id}/toggle")
    public String toggleRoomActive(@PathVariable Long id) {
        roomService.toggleActive(id);
        return "redirect:/admin/rooms";
    }
}
