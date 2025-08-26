package gr.hua.dit.rentEstate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)   //Για να μη χτυπάει σε περίπτωση που τρέξουν δεύτερη φορά
@SpringBootTest
@AutoConfigureMockMvc
class RentEstatesApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		// Έλεγχος ότι το Spring context φορτώνει
	}

	@Test
	public void testCreateUser() throws Exception{
		mockMvc.perform(post("/saveUser")
						.param("username", "apiuser")
						.param("email", "api@hua.gr")
						.param("password", "pass123")
						.with(csrf())) // απαραίτητο αν CSRF είναι ενεργό
				.andExpect(status().is3xxRedirection()) // 302 redirect
				.andExpect(redirectedUrl("/login"));  // redirect στο login
	}


	@Test
	public void testLoginWithForm() throws Exception {
		// Πρώτα πρέπει να υπάρχει ο χρήστης (ή να τρέξεις το προηγούμενο test)
		mockMvc.perform(post("/postLogin")
						.param("username", "formuser")
						.param("password", "pass123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection()); // redirect μετά το login
	}


	@Test
	public void testSignupAndAccessUserProfile() throws Exception {
		//Δημιουργία χρήστη
		mockMvc.perform(post("/saveUser")
						.param("username", "formuser")
						.param("email", "api@hua.gr")
						.param("password", "pass123")
						.with(csrf())) // απαραίτητο για POST σε Spring Security
				.andExpect(status().is3xxRedirection()); // redirect μετά το signup

		//Κάνουμε login (form login)
		mockMvc.perform(post("/postLogin")
						.param("username", "formuser")
						.param("password", "pass123")
						.with(csrf()))
				.andExpect(status().is3xxRedirection()); // redirect μετά το login

		//Προσπελάβνουμε (/user)
		mockMvc.perform(get("/user")
						.with(user("formuser").password("pass123").roles("USER"))) // logged-in context
				.andExpect(status().isOk());
	}

}
