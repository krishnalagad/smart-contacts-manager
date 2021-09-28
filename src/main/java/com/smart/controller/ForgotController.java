package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {
	
	Random random = new Random(1000);
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepositiry;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

//	Email ID form open handler
	
	@RequestMapping("/forgot")
	public String openEmailForm() {
		
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		
		System.out.println("Email : " + email);
		
//		Generating OTP
		
		int otp = random.nextInt(999999);		
		
		System.out.println("OTP : " + otp);
		
//		Code for send OTP to email.
		
		String subject = "OTP From SCM";
		String message = "<div style='border:1px solid #e2e2e2; padding:20px;'>"
						+ "<h3>"
						+ "Your OTP is "
						+ "<b>"
						+ otp
						+ "</b>"
						+ "</h3>"
						+ "</div>";
						
		String to = email;
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		
		if(flag) {
			
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		}else
		{
			session.setAttribute("message", "Check Your email Id!!");
			return "forgot_email_form";
		}	
		
		
	}
	
//	verify otp
	
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		
		int myOtp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");
		
		if(myOtp == otp) {
			
//			password change form
			User user = this.userRepositiry.getUserByUserName(email);
			
			if(user == null) {
//				Send error message
				
				session.setAttribute("message", "No user exists with this Email !!");
				return "forgot_email_form";
			}
			else {
//				send change password form.
			}
			
			return "password_change_form";
		}
		else {
			
			session.setAttribute("message", "You have entered wrong OTP.");
			return "verify_otp";
		}
	}
	
//	Change password
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session) {
		
		String email = (String) session.getAttribute("email");
		User user = this.userRepositiry.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
		this.userRepositiry.save(user);
		
		return "redirect:/signin?change=password changed succesfully..";
	}
	
	
}
