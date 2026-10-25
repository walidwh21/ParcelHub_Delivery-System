package com.parcelhub.logistics.controller;

import com.parcelhub.logistics.util.SessionUtil;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class PublicController {

	@GetMapping("/")
	public String home(Model model) {
		if (SessionUtil.hasActiveSession()) {
			List<String> userRoles = SessionUtil.currentUserRoles();
			if (userRoles.contains("ROLE_ADMIN")) return "redirect:/admin/dashboard";
			if (userRoles.contains("ROLE_USER")) return "redirect:/user/dashboard";
			if (userRoles.contains("ROLE_RIDER")) return "redirect:/rider/dashboard";
		}
		model.addAttribute("title", "Dashboard - Home");
		return "index";
	}
}
