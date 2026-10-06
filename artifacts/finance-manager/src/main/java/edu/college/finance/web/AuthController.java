package edu.college.finance.web;

import edu.college.finance.entity.User;
import edu.college.finance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping({"/", "/login"})
    public String login(HttpServletRequest request) {
        if (request.getSession(false) != null && request.getSession(false).getAttribute("userId") != null)
            return "redirect:/dashboard";
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String identity, @RequestParam String password,
                        HttpServletRequest request, Model model) {
        String key = identity.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmailIgnoreCase(key).or(() -> users.findByUsernameIgnoreCase(key)).orElse(null);
        if (user == null || !encoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "That email/username and password combination was not found.");
            model.addAttribute("identity", identity);
            return "auth/login";
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        return "redirect:/dashboard";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("form", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("form") @Valid SignupForm form, BindingResult result, Model model,
                         HttpServletRequest request) {
        if (users.existsByEmailIgnoreCase(form.getEmail())) result.rejectValue("email", "duplicate", "Email is already registered.");
        if (users.existsByUsernameIgnoreCase(form.getUsername())) result.rejectValue("username", "duplicate", "Username is already taken.");
        if (!form.getPassword().equals(form.getConfirmPassword())) result.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        if (result.hasErrors()) return "auth/signup";
        User user = new User();
        user.setName(form.getName().trim());
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPassword(encoder.encode(form.getPassword()));
        user = users.save(user);
        request.getSession(true).setAttribute("userId", user.getId());
        request.getSession().setAttribute("userName", user.getName());
        return "redirect:/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        return "redirect:/login?logout";
    }

    public static class SignupForm {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 80)
        private String name;
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9_]{3,40}$", message = "Use 3–40 letters, numbers or underscores.")
        private String username;
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.NotBlank
        private String email;
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 8, max = 72, message = "Use at least 8 characters.")
        private String password;
        @jakarta.validation.constraints.NotBlank
        private String confirmPassword;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
