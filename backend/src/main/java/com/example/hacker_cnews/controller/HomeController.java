package com.example.hacker_cnews.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

@Controller
public class HomeController {
    
    @GetMapping(value = "/old_index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Mono<Resource> home() {
        return Mono.just(new ClassPathResource("static/index.html"));
    }
} 