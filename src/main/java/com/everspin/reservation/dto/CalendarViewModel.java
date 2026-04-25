package com.everspin.reservation.dto;

import com.everspin.reservation.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record CalendarViewModel(
        int currentYear,
        int currentMonth,
        String currentMonthName,
        int daysInMonth,
        int firstDayOfWeek,
        int todayDay,
        boolean isCurrentMonth,
        int prevYear,
        int prevMonth,
        int nextYear,
        int nextMonth,
        Map<Integer, List<Reservation>> calendarMap
) {
    public static CalendarViewModel of(int year, int month, Map<Integer, List<Reservation>> calendarMap) {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.of(year, month);
        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);

        return new CalendarViewModel(
                year,
                month,
                ym.getMonth().getDisplayName(TextStyle.FULL, Locale.KOREAN),
                ym.lengthOfMonth(),
                ym.atDay(1).getDayOfWeek().getValue() % 7,
                today.getDayOfMonth(),
                year == today.getYear() && month == today.getMonthValue(),
                prev.getYear(),
                prev.getMonthValue(),
                next.getYear(),
                next.getMonthValue(),
                calendarMap
        );
    }
}
