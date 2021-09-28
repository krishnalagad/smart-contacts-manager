package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
//	method for adding common data to responce.
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME : " + userName);
		
//		get the user using username(email)

		
		User user = this.userRepository.getUserByUserName(userName);
		
		System.out.println("USER : " + user);
		
		model.addAttribute("user", user);
		
	}
	
//	dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
		model.addAttribute("title", "User Dashboard");

		return "normal/user_dashboard";
	}
	
	
//	open add from handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		
		return "normal/add_contact_form";
	}
	
//	processing add contact form.
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, 
								@RequestParam("profileImage") MultipartFile file, 
								Principal principal, 
								HttpSession session) {
		
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
//			Processing and uploading file.
			
			if(file.isEmpty()) {
//				If the file is empty.
				System.out.println("File is empty..");
//				contact.setImage("contact.png");
			}
			else {
//				Upload the file to the folder and update the name to contact.
				contact.setImage(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is uploaded..");
			}
			
			contact.setUser(user);
			
			user.getContacts().add(contact);
			
			this.userRepository.save(user);
			
			System.out.println("Data : " + contact);
			
			System.out.println("Added to Database..");
			
//			message success.
			session.setAttribute("message", new Message("Your contact is added !! Add more", "success"));
			
		}
		catch(Exception e) {
			System.out.println("ERROR : " + e.getMessage());
			e.printStackTrace();
			
//			message error.
			session.setAttribute("message", new Message("Something went Wrong !! Try again", "danger"));
		}
		
		return "normal/add_contact_form";
	}
	
//	Show contact handler.
	
//	per page = 5[n]
//	current page = 0 [page]
	
	@GetMapping("/show-contacts/{page}")
	public String showContact(@PathVariable("page") Integer page, Model m, Principal principal) {
		
		m.addAttribute("title", "Show User Contacts");
	
//		send list of contacts to the show_contacts page.

//		1st method
//		String userName = principal.getName();
//		
//		User user = this.userRepository.getUserByUserName(userName);
//		List<Contact> contacts = user.getContacts();
		
//		2nd method
		
		String userName = principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
		
//		current page-page
//		contact per page-5
		Pageable pageable = PageRequest.of(page, 10);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		
		
		
		return "normal/show_contacts";
	}
	
//	showing perticular contact details..
	
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		
		model.addAttribute("title", "Contact Details");
		
		System.out.println("CID : " + cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
//		Get user to verify
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		
		
		return "normal/contact_detail";
	}
	
//	Delete contact handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, 
								Model model, 
								HttpSession session, 
								Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
//		contact.setUser(null);
		
//		check..
		
//		this.contactRepository.delete(contact);
		
		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		
//		Delete image
		
		File deleteFile;
		try {
			deleteFile = new ClassPathResource("static/img").getFile();
			File file1 = new File(deleteFile, contact.getImage());
			file1.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("Deleted");
		session.setAttribute("message", new Message("Contact Deleted Succesfully", "success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
//	Open update form handler
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		
		m.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
//	Update Contact Handler
	
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updatehandler(@ModelAttribute Contact contact, 
								@RequestParam("profileImage") MultipartFile file,
								Model m,
								HttpSession session,
								Principal principal) 
	{
		try {
			
//			get old contact details
			
			Contact oldcontactDetails = this.contactRepository.findById(contact.getcId()).get();
			
//			image
			if(!file.isEmpty()) {
//				File work..
//				Rewrite..
				
//				delete old image
				
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldcontactDetails.getImage());
				file1.delete();
				
//				update new photo
				
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}
			else {
				contact.setImage(oldcontactDetails.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated..", "success"));
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Contact Name : " + contact.getName());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
//	Your profile handler
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title", "Your Profile");
		return "normal/profile";
	}
	
//	open setting handler
	
	@GetMapping("/settings")
	public String openSettings() {
		
		return "normal/settings";
	}
	
//	change password handler
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
								@RequestParam("newPassword") String newPassword,
								Principal principal,
								HttpSession session) {
		
		
//		System.out.println("OLD : " + oldPassword);
//		System.out.println("NEW : " + newPassword);
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		
//		Current user password
//		System.out.println("Current Pass : " + currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			
//			Change the passowrd
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your Password is Succesfully Changed", "success"));
		}
		else {
//			error
			session.setAttribute("message", new Message("Your Old Password is Incorrect ", "danger"));
			return "redirect:/user/settings";
		}		
		
		return "redirect:/user/index";
		
	}
		
}
