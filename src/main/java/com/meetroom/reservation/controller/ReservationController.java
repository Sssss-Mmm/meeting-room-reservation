package com.meetroom.reservation.controller;

import com.meetroom.reservation.domain.Reservation;
import com.meetroom.reservation.dto.CalendarViewModel;
import com.meetroom.reservation.dto.ReservationRequest;
import com.meetroom.reservation.security.CustomUserDetails;
import com.meetroom.reservation.service.ReservationService;
import com.meetroom.reservation.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final RoomService roomService;

    @GetMapping
    public String myReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            Model model) {

        LocalDate today = LocalDate.now();
        int y = (year == 0) ? today.getYear()  : year;
        int m = (month == 0) ? today.getMonthValue() : month;
        Long userId = userDetails.getUser().getId();

        model.addAttribute("calendar",          CalendarViewModel.of(y, m, reservationService.findMonthlyCalendar(userId, y, m)));
        model.addAttribute("statusCount",       reservationService.countByStatus(userId));
        model.addAttribute("todayReservations", reservationService.findTodayReservations());
        return "reservation/list";
    }

    @GetMapping("/new")
    public String newReservationForm(Model model) {
        model.addAttribute("reservationRequest", new ReservationRequest());
        model.addAttribute("rooms", roomService.findActiveRooms());
        return "reservation/form";
    }

    @PostMapping("/new")
    public String submitReservation(@Valid @ModelAttribute ReservationRequest request, BindingResult result,
                                    @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("rooms", roomService.findActiveRooms());
            return "reservation/form";
        }

        try {
            reservationService.createReservation(userDetails.getUser(), request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("rooms", roomService.findActiveRooms());
            model.addAttribute("errorMessage", e.getMessage());
            return "reservation/form";
        }

        return "redirect:/reservations?success=true";
    }

    @GetMapping("/{id}")
    public String viewReservation(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Reservation reservation = reservationService.findById(id);
        if (!reservation.getUser().getId().equals(userDetails.getUser().getId())) {
            return "redirect:/reservations";
        }
        model.addAttribute("reservation", reservation);
        return "reservation/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        reservationService.cancelReservation(id, userDetails.getUser().getId());
        return "redirect:/reservations";
    }

    // 거부된 요청의 출구를 한 곳으로 모은다 — 없는 예약 조회/취소가 500으로 새지 않게
    // (폼 제출은 입력값을 살려 다시 그려야 하므로 submitReservation이 직접 처리한다)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleRejectedRequest(RuntimeException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/reservations";
    }
}
