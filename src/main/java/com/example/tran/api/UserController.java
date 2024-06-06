package com.example.tran.api;

import com.example.tran.entity.User;
import com.example.tran.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/user/list")
    public ResponseEntity<List<User>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/user/check")
    public ResponseEntity<String> check() throws Exception {
        userService.check();
        return ResponseEntity.ok("success");
    }
}
