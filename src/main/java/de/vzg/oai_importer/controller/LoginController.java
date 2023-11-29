package de.vzg.oai_importer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class LoginController {

    @RequestMapping(value = "/login", method = { RequestMethod.GET})
    private String login() {
        // TODO Auto-generated method stub
        return "login";
    }

    @RequestMapping(value = "/logout", method = { RequestMethod.GET})
    private String logout() {
        // TODO Auto-generated method stub
        return "logout";
    }
}
