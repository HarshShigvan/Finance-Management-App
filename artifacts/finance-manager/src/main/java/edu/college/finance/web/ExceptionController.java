package edu.college.finance.web;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ExceptionController {
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NoSuchElementException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public String duplicate(org.springframework.dao.DataIntegrityViolationException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "That category already has a budget for the selected month.");
        return "redirect:/budgets";
    }
}
