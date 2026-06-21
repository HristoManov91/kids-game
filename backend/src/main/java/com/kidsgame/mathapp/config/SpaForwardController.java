package com.kidsgame.mathapp.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
            "/",
            "/login",
            "/register",
            "/forgot-password",
            "/reset-password",
            "/account",
            "/app",
            "/alphabet",
            "/numbers",
            "/report",
            "/rewards/album",
            "/rewards/album/new",
            "/admin/monitoring",
            "/admin/catalog",
            "/admin/reward-catalog",
            "/quiz/{attemptId}",
            "/result/{attemptId}",
            "/rewards/album/{pictureId}",
            "/admin/monitoring/attempts/{attemptId}"
    })
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
