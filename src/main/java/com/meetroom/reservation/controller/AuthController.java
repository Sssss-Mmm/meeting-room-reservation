package com.meetroom.reservation.controller;

import com.meetroom.reservation.dto.SignupRequest;
import com.meetroom.reservation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String processSignup(@Valid @ModelAttribute SignupRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        if (userService.existsByUsername(request.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
            return "auth/signup";
        }

        if (userService.existsByEmail(request.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "이미 사용 중인 이메일입니다.");
            return "auth/signup";
        }

        userService.register(request);
        return "redirect:/auth/login?registered=true";
    }
}
