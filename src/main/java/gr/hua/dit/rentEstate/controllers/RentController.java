package gr.hua.dit.rentEstate.controllers;

import gr.hua.dit.rentEstate.entities.*;
import gr.hua.dit.rentEstate.service.EstateService;
import gr.hua.dit.rentEstate.service.RentService;
import gr.hua.dit.rentEstate.service.TenantService;
import gr.hua.dit.rentEstate.service.UserService;
import gr.hua.dit.rentEstate.service.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.ArrayList;
import java.util.List;


@Controller
@RequestMapping("rent")
public class RentController {

    //Service classes
    private final EstateService estateService;
    private final RentService rentService;
    private final TenantService tenantService;
    private final UserService userService;
    private final EmailService emailService;

    // Constructor to initialize services
    public RentController(EstateService estateService, RentService rentService, TenantService tenantService, UserService userService, EmailService emailService) {
        this.estateService = estateService;
        this.rentService = rentService;
        this.tenantService = tenantService;
        this.userService = userService;
        this.emailService = emailService;
    }

    // Rent request form
    @GetMapping("/form")
    public String showRentForm(@RequestParam("estateId") Integer estateId, Model model) {
        Estate estate = estateService.getEstate(estateId);
        if (estate == null) {
            throw new IllegalArgumentException("Invalid estate ID");
        }
        model.addAttribute("estate", estate);
        model.addAttribute("rent", new Rent()); // Create a new Rent object for the form
        return "estate/request";
    }

    // View the list of rent requests for a specific owner and estate
    @GetMapping("/rent")
    public String viewOwnerRents(@RequestParam("ownerId") Long ownerId, Long estateId, Model model) {
        List<Rent> rent = rentService.getRentsByOwnerId(ownerId, estateId);
        model.addAttribute("rent", rent);
        model.addAttribute("ownerId", ownerId);
        model.addAttribute("estateId", estateId);
        return "estate/rentList";
    }

    // Approve a rent request
    @PostMapping("/approve")
    public String approveRent(@RequestParam("rentId") Long rentId, @RequestParam("ownerId") Long ownerId, @RequestParam("estateId") Integer estateId) {
        Rent rent = rentService.getRentById(rentId);
        if (rent == null) {
            throw new IllegalArgumentException("Invalid rent ID");
        }

        Estate estate = estateService.getEstate(estateId);
        if (estate == null) {
            throw new IllegalArgumentException("Invalid estate ID");
        }

        String tenantUsername = rent.getTenantUsername();
        Tenant tenant = tenantService.findByUsername(tenantUsername);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found");
        }

        // Associate the estate with the tenant
        if (tenant.getEstates() == null) {
            tenant.setEstates(new ArrayList<>());
        }
        tenant.getEstates().add(estate);
        estate.setTenant(tenant);

        tenantService.saveTenant(tenant); // Save tenant changes
        rentService.approveRent(rentId);  // Approve the rent request

        // Email στον ενοικιαστή
        User tenantUser = userService.getUserByUsername(tenantUsername);
        if (tenantUser != null) {
            String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#2c3e50;">Έγκριση Αίτησης Ενοικίασης </h2>
                    <p>Γεια σου <b>%s</b>,</p>
                    <p>Η αίτησή σου για το ακίνητο <b>%s</b> εγκρίθηκε από τον ιδιοκτήτη.</p>
                    <p>Μπορείς πλέον να δεις τα στοιχεία στο προφίλ σου.</p>
                    <br>
                    <p style="color:gray; font-size: 0.9em;">Μην απαντάς σε αυτό το email.</p>
                </body>
                </html>
            """.formatted(tenantUser.getUsername(), estate.getStreetName() + " " + estate.getStreetNumber());


            emailService.sendHtmlEmail(tenantUser.getEmail(), " Έγκριση Αίτησης Ενοικίασης", html);
        }

        return "redirect:/rent/rent?estateId=" + estateId + "&ownerId=" + ownerId; // Redirect to the rent list
    }

    // Reject a rent request
    @PostMapping("/reject")
    public String rejectRent(@RequestParam("rentId") Long rentId, @RequestParam("ownerId") Long ownerId, @RequestParam("estateId") Long estateId) {
        rentService.rejectRent(rentId); // Reject the rent request
        Rent rent = rentService.getRentById(rentId);

        // Email στον ενοικιαστή
        User tenantUser = userService.getUserByUsername(rent.getTenantUsername());
        if (tenantUser != null) {
            String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#2c3e50;">Απόρριψη Αίτησης Ενοικίασης </h2>
                    <p>Γεια σου <b>%s</b>,</p>
                    <p>Δυστυχώς, η αίτησή σου για το ακίνητο απορρίφθηκε από τον ιδιοκτήτη.</p>
                    <p>Μπορείς να υποβάλεις νέα αίτηση σε διαφορετικό ακίνητο.</p>
                    <br>
                    <p style="color:gray; font-size: 0.9em;">Μην απαντάς σε αυτό το email.</p>
                </body>
                </html>
            """.formatted(tenantUser.getUsername());


            emailService.sendHtmlEmail(tenantUser.getEmail(), " Απόρριψη Αίτησης Ενοικίασης", html);
        }


        return "redirect:/rent/rent?estateId=" + estateId + "&ownerId=" + ownerId; // Redirect to the rent list
    }

    // Submit a new rent request
    @PostMapping("/submit")
    public String submitRentRequest(@ModelAttribute Rent rent, @RequestParam("estateId") Integer estateId, Model model, Principal principal) {
        String loggedInUsername = principal.getName(); // Get the logged-in user's username

        Estate estate = estateService.getEstate(estateId);
        if (estate == null) {
            throw new IllegalArgumentException("Invalid estate ID");
        }

        Tenant tenant = tenantService.findByUsername(loggedInUsername);
        if (tenant == null) {
            // If the tenant doesn't exist, create a new one
            tenant = new Tenant();
            tenant.setTenantUsername(loggedInUsername);
            rent.setTenantUsername(loggedInUsername);
            User user = userService.getUserByUsername(loggedInUsername);
            if (user != null) {
                tenant.setEmail(user.getEmail()); // Set email from user info
            }
            estate.setTenant(tenant); // Associate tenant with estate
            tenantService.saveTenant(tenant);
        }

        rent.setEstate(estate); // Link the rent request to the estate
        rent.setTenantUsername(loggedInUsername); // Set the tenant's username
        rentService.createRent(rent); // Save the rent request

        // Email στον ιδιοκτήτη
        User ownerUser = userService.getUserByUsername(estate.getOwnerName());
        if (ownerUser != null) {
            String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#2c3e50;">Νέα Αίτηση Ενοικίασης </h2>
                    <p>Αγαπητέ <b>%s</b>,</p>
                    <p>Ο χρήστης <b>%s</b> υπέβαλε αίτηση για το ακίνητο σου.</p>
                    <p>Μπορείς να την δεις και να την διαχειριστείς στην πλατφόρμα.</p>
                    <br>
                    <p style="color:gray; font-size: 0.9em;">Μην απαντάς σε αυτό το email.</p>
                </body>
                </html>
            """.formatted(ownerUser.getUsername(), loggedInUsername);


            emailService.sendHtmlEmail(ownerUser.getEmail(), " Νέα Αίτηση Ενοικίασης", html);
        }

        model.addAttribute("message", "Your rent request has been submitted successfully.");
        return "estate/success";
    }

    // View the logged-in user's rental requests
    @GetMapping("/my-rentals")
    public String viewMyRentals(Model model, Principal principal) {
        String loggedInUsername = principal.getName(); // Get logged-in username

        Tenant tenant = tenantService.findByUsername(loggedInUsername);
        List<Rent> MyRentRequests = rentService.getRentsByTenantUsername(loggedInUsername);

        if (MyRentRequests.isEmpty()) {
            model.addAttribute("msg", "There are no Rent Requests.");
        }

        model.addAttribute("rentRequests", MyRentRequests); // Add rental requests
        return "estate/myRentals";
    }
}
