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
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
public class UserController {

    private final UserService userService;
    private final EstateService estateService;
    private final RentService rentService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    public UserController(UserService userService,
                          RoleRepository roleRepository,
                          EstateService estateService,
                          RentService rentService,
                          EmailService emailService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.estateService = estateService;
        this.rentService = rentService;
        this.emailService = emailService;
    }

    // Εγγραφή νέου χρήστη
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    // Αποθήκευση νέου χρήστη + email
    @PostMapping("/saveUser")
    public String saveUser(@ModelAttribute User user, Model model) {
        user.setVerified(false); // default μη-εγκεκριμένος
        userService.saveUser(user);

        // HTML Email μετά την εγγραφή
        String htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color:#2c3e50;">Καλώς ήρθες στο <span style="color:#3498db;">RentEstate</span>!</h2>
                <p>Γεια σου <b>%s</b>,</p>
                <p>Η εγγραφή σου ολοκληρώθηκε με επιτυχία 🎉</p>
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

        model.addAttribute("msg", "Ο λογαριασμός δημιουργήθηκε. Περιμένετε έγκριση από διαχειριστή.");
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

    // Εμφάνιση όλων των χρηστών (Admin)
    @GetMapping("/users")
    public String showUsers(Model model) {
        List<User> users = userService.getUsers();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().toString();

        if (role.contains("ROLE_ADMIN")) {
            for (User user : users) {
                if (user.isVerified() == null) user.setVerified(false);
            }
            model.addAttribute("users", users);
            model.addAttribute("roles", roleRepository.findAll());
            return "auth/users";
        }

        return "error/error-403";
    }

    // Εμφάνιση συγκεκριμένου χρήστη
    @GetMapping("/user/{user_id}")
    public String showUser(@PathVariable Long user_id, Model model){
        model.addAttribute("user", userService.getUser(user_id));
        return "auth/user";
    }

    // Προφίλ τρέχοντος χρήστη
    @GetMapping("/user")
    public String getCurrentUserProfile(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);
        return "auth/user";
    }

    // Ενημέρωση χρήστη (email, username)
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

        Authentication newAuth = new UsernamePasswordAuthenticationToken(the_user, authentication.getCredentials(), authentication.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return "redirect:/user";
    }

    @GetMapping("/logout")
    public String logout() {
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }
}