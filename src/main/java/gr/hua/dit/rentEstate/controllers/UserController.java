package gr.hua.dit.rentEstate.controllers;

import gr.hua.dit.rentEstate.entities.User;
import gr.hua.dit.rentEstate.repositories.RoleRepository;
import gr.hua.dit.rentEstate.service.EmailService;
import gr.hua.dit.rentEstate.service.EstateService;
import gr.hua.dit.rentEstate.service.RentService;
import gr.hua.dit.rentEstate.service.UserService;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.List;


@Controller
public class UserController {

    private UserService userService;
    private EstateService estateService;
    private RentService rentService;

    private RoleRepository roleRepository;
    private EmailService emailService;

    public UserController(UserService userService, RoleRepository roleRepository,EstateService estateService, RentService rentService,EmailService emailService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.estateService = estateService;
        this.rentService = rentService;
        this.emailService = emailService;
    }

    //Register
    @GetMapping("/register")
    public String register(Model model) {
        User user = new User();
        model.addAttribute("user", user);
        return "auth/register";
    }

    //Save new User
    @PostMapping("/saveUser")
    public String saveUser(@ModelAttribute User user, Model model){
        user.setVerified(false);// Set User as Non-Verified
        Integer id = userService.saveUser(user);

        // HTML Email μετά την εγγραφή
        String htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color:#2c3e50;">Καλώς ήρθες στο <span style="color:#3498db;">RentEstate</span>!</h2>
                <p>Γεια σου <b>%s</b>,</p>
                <p>Η εγγραφή σου ολοκληρώθηκε με επιτυχία !!</p>
                <p>Μπορείς να συνδεθείς στην πλατφόρμα, αλλά για να αποκτήσεις πρόσβαση 
                σε βασικές λειτουργίες (όπως αιτήσεις ή αγγελίες), πρέπει πρώτα να εγκριθείς από τον διαχειριστή.</p>
                <br>
                <p style="color:gray; font-size: 0.9em;">Μην απαντάς σε αυτό το email.</p>
            </body>
            </html>
        """.formatted(user.getUsername());


        emailService.sendHtmlEmail(
                user.getEmail(),
                "Η εγγραφή σου στο RentEstate!",
                htmlContent
        );

        model.addAttribute("msg", "Your account has been created but is unverified. Please wait for admin approval.");
        return "redirect:/login";
    }

    // ✅ Εγκριση χρήστη από admin + email
    @PostMapping("/users/verify/{id}")
    public String verifyUser(@PathVariable("id") Long userId) {
        User user = userService.findById(userId);
        if (user != null) {
            user.setVerified(true);
            userService.updateUser(user);


            // HTML Email μετά την έγκριση
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#2c3e50;">Ο λογαριασμός σου εγκρίθηκε ✅</h2>
                    <p>Γεια σου <b>%s</b>,</p>
                    <p>Ο διαχειριστής ενέκρινε τον λογαριασμό σου. Μπορείς πλέον να χρησιμοποιήσεις όλες τις λειτουργίες της πλατφόρμας!</p>
                    <br>
                    <p style="color:gray; font-size: 0.9em;">Αυτό το email στάλθηκε αυτόματα.</p>
                </body>
                </html>
            """.formatted(user.getUsername());


            emailService.sendHtmlEmail(
                    user.getEmail(),
                    "Ο λογαριασμός σου εγκρίθηκε – RentEstate",
                    htmlContent
            );
        }


        return "redirect:/users";
    }


    //Show All Users - Only for Admin
    @GetMapping("/users")
    public String showUsers(Model model) {
        List<User> users = userService.getUsers();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().toString();

        if (role.contains("ROLE_ADMIN")) { //Check if logged-in user is admin


            for (User user : users) {
                if (user.isVerified() == null) {
                    user.setVerified(false);
                }
            }


            model.addAttribute("users", users);
            model.addAttribute("roles", roleRepository.findAll());

            return "auth/users"; // If logged-in user is admin return users
        }
        else if (role.contains("ROLE_USER")) { // If logged-in user is not admin return error-4043 no permission
            return "error/error-403";
        }
        return "error/error-403";

    }

    @GetMapping("/user/{user_id}")
    public String showUser(@PathVariable Long user_id, Model model){
        model.addAttribute("user", userService.getUser(user_id));
        return "auth/user";
    }

    @GetMapping("/logout")
    public String logout() {
        SecurityContextHolder.clearContext();
        return "redirect:/"; // Redirect to home
    }

    //Save updated User
    @PostMapping("/user/{user_id}")
    public String saveUser(@PathVariable Long user_id,
                           @ModelAttribute("user") User user,
                           Model model,
                           Authentication authentication) {
        User the_user = (User) userService.getUser(user_id);
        String oldUsername = the_user.getUsername();


        the_user.setEmail(user.getEmail());
        the_user.setUsername(user.getUsername());
        userService.updateUser(the_user);


        estateService.updateOwnerUsernameInEstates(oldUsername, the_user.getUsername());
        rentService.updateRentTenantUsername(oldUsername, the_user.getUsername());

        // Refresh the authentication object with the new user data
        Authentication newAuth = new UsernamePasswordAuthenticationToken(the_user, authentication.getCredentials(), authentication.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        model.addAttribute("users", userService.getUsers());

        // Redirect to user's profile page or any other page you want
        return "redirect:/user";
    }





    //Get User profile
    @GetMapping("/user")
    public String getCurrentUserProfile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);
        return "auth/user";
    }

    



}


