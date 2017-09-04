package com.weather.controller;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.weather.model.User;
import com.weather.service.EmailService;
import com.weather.service.UserService;

@Controller
public class LoginController {

	private BCryptPasswordEncoder bCryptPasswordEncoder;
	private UserService userService;
	private EmailService emailService;

	@Autowired
	public LoginController(BCryptPasswordEncoder bCryptPasswordEncoder, UserService userService,
			EmailService emailService) {
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
		this.userService = userService;
		this.emailService = emailService;
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView showRegistrationPage(ModelAndView modelAndView, User user) {

		modelAndView.setViewName("login");
		return modelAndView;
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ModelAndView processRegistrationForm(ModelAndView modelAndView, @Valid User user,
			BindingResult bindingResult, HttpServletRequest request) {

		User userExists = userService.findByEmail(user.getEmail());

		System.out.println(userExists);

		

		userExists.setConfirmationToken(UUID.randomUUID().toString());
		userExists.setLoginExpiry(new Date());
		userService.saveUser(userExists);

		String appUrl = request.getScheme() + "://" + request.getServerName();

		SimpleMailMessage registrationEmail = new SimpleMailMessage();
		registrationEmail.setTo(user.getEmail());
		registrationEmail.setSubject("Login Url");
		registrationEmail.setText("Please login using below url:\n" + appUrl
				+ ":8080/confirm?token=" + userExists.getConfirmationToken());
		registrationEmail.setFrom("noreply@domain.com");
		emailService.sendEmail(registrationEmail);

		modelAndView.addObject("confirmationMessage", "Login URL has been sent to your email : " + userExists.getEmail());
		//modelAndView.setViewName("details");

		return modelAndView;

	}

	// Process confirmation link
	@RequestMapping(value = "/confirm", method = RequestMethod.GET)
	public ModelAndView confirmRegistration(ModelAndView modelAndView, @RequestParam("token") String token) {

		User user = userService.findByConfirmationToken(token);

		if (user == null) { // No token found in DB
			modelAndView.addObject("invalidToken", "Oops!  This is an invalid confirmation link.");
		} else { // Token found
			int diffInMins = (int)( (user.getLoginExpiry().getTime() - new Date().getTime()) 
	                / (1000 * 60 ) ) ;
			if (diffInMins > 15)
				modelAndView.addObject("expiryToken", "Oops!  This token has been expired");
			modelAndView.addObject("confirmationToken", user.getConfirmationToken());
		}
		
		
		modelAndView.addObject("userObject" , user);
		modelAndView.setViewName("details");
		return modelAndView;
	}

}