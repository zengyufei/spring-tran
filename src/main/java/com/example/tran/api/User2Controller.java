package com.example.tran.api;

import com.example.tran.entity.User2;
import com.example.tran.service.User2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class User2Controller {

    private final User2Service user2Service;

    @GetMapping("/user2/list")
    public ResponseEntity<List<User2>> list() {
        return ResponseEntity.ok(user2Service.findAll());
    }
}
