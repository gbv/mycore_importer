package de.vzg.oai_importer.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MyErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == 404) {
                return "error_404";
            } else if (statusCode == 403) {
                return "error_403";
            }
        }
        model.addAttribute("message", getMessage(request));
        return "error";
    }

    /**
     * Collects the messages of the exception which caused the error and of its causes, so that the user sees the
     * actual reason (e.g. a missing mapping group) and not only a generic error page.
     */
    private static String getMessage(HttpServletRequest request) {
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (exception instanceof Throwable throwable) {
            List<String> messages = new ArrayList<>();
            for (Throwable current = throwable; current != null
                && messages.size() < 5; current = current.getCause() == current ? null : current.getCause()) {
                String message = current.getMessage();
                if (message != null && !message.isBlank() && !messages.contains(message)) {
                    messages.add(message);
                }
            }
            if (!messages.isEmpty()) {
                return String.join("\n", messages);
            }
            return throwable.getClass().getSimpleName();
        }
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        return message == null || message.toString().isBlank() ? null : message.toString();
    }
}
