package com.technicalchallenge.controller;

import com.technicalchallenge.service.AuthorizationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
@Validated
@AllArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;


    @PostMapping("/{user}")
    public ResponseEntity<?> login(
            @PathVariable(name = "user") String username, 
            @RequestParam(name = "Authorization") String password) {

        return authorizationService.authenticateUser(username, password) ?
                ResponseEntity.ok("Login successful") :
                ResponseEntity.status(HttpStatus.FORBIDDEN).body("Login failed");
    }


}