package gr.hua.dit.rentEstate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultActions;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)   //Για να μη χτυπάει σε περίπτωση που τρέξουν δεύτερη φορά
@SpringBootTest
@AutoConfigureMockMvc
class RentEstatesApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {

	}

	@Test
	public void testCreateUser() throws Exception{
		mockMvc.perform(post("/saveUser")
						.param("username", "apiuser")
						.param("email", "api@hua.gr")
						.param("password", "123456")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));  // redirect στο login
	}


	@Test
	public void testLoginWithForm() throws Exception {
		// Προσοχή πρέπει να υπάρχει ο χρήστης ή να τρέξει to testCreateUser
		mockMvc.perform(post("/postLogin")
						.param("username", "formuser")
						.param("password", "123456")
						.with(csrf()))
				.andExpect(status().is3xxRedirection()); // redirect μετά το login
	}


	@Test
	public void testSignupAndAccessUserProfile() throws Exception {
		//Δημιουργία χρήστη
		mockMvc.perform(post("/saveUser")
						.param("username", "formuser")
						.param("email", "formuser@hua.gr")
						.param("password", "123456")
						.with(csrf()))
				.andExpect(status().is3xxRedirection()); // redirect μετά το signup

		//Κάνουμε login (form login)
		mockMvc.perform(post("/postLogin")
						.param("username", "formuser")
						.param("password", "123456")
						.with(csrf()))
				.andExpect(status().is3xxRedirection()); // redirect μετά το login

		//Προσπελάβνουμε (/user)
		mockMvc.perform(get("/user")
						.with(user("formuser").password("123456").roles("USER")))
				.andExpect(status().isOk());
	}

	@Test
	public void testLogout() throws Exception {
		mockMvc.perform(get("/logout"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	@WithMockUser(username = "flowuser", roles = {"USER"})  // ως "authenticated" user
	public void testCompleteUserFlow() throws Exception {
		//Signup
		mockMvc.perform(post("/saveUser")
						.with(csrf())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "flowuser")
						.param("email", "flowuser@hua.gr")
						.param("password", "123456"))
				.andExpect(status().is3xxRedirection()); // redirect μετά τη δημιουργία χρήστη

		//Login
		ResultActions loginResult = mockMvc.perform(post("/postLogin")
						.with(csrf())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "flowuser")
						.param("password", "123456"))
				.andExpect(status().is3xxRedirection()); // redirect Με βάση  το role

		//Access USER profile
		mockMvc.perform(get("/user")
						.principal(() -> "flowuser")) // προσομοιωση logged  in user
				.andExpect(status().isOk());

		//Logout
		mockMvc.perform(get("/logout"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	@WithMockUser(username = "flowuser", roles = {"USER"})
	public void testLoadNewTenantForm() throws Exception {
		mockMvc.perform(get("/tenant/new"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("tenant"))
				.andExpect(view().name("tenant/tenant"));
	}

	@Test
	@WithMockUser(username = "flowuser", roles = {"USER"})
	public void testSaveTenant() throws Exception {
		mockMvc.perform(post("/tenant/new")
						.with(csrf())
						.param("tenantUsername", "George Sakkos")
						.param("email", "skk@137.com"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("tenants"))
				.andExpect(view().name("tenant/tenants"));
	}

}