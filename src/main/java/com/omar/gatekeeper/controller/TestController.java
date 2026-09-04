package com.omar.gatekeeper.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/api/test")
    public String healthCheck() {
        return "GateKeeper Server is up and running!";
    }
}
