package edu.college.finance.web;

import edu.college.finance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
    private final UserRepository users;

    public GlobalModelAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute("currentUser")
    public Object currentUser(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) return null;
        return users.findById((Long) session.getAttribute("userId")).orElse(null);
    }
}
