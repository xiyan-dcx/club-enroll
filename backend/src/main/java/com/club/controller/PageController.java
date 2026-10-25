package com.club.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller public class PageController {
 @GetMapping({"/login","/register","/student/**","/admin/**"}) public String page(){return "forward:/index.html";}
}
