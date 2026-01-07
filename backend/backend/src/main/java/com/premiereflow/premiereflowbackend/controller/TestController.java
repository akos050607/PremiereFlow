package com.premiereflow.premiereflowbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String elsoTeszt() {
        return "Szia! Működik a PremiereFlow backend!";
    }
}