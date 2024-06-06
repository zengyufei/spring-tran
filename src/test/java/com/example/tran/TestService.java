package com.example.tran;

import com.example.tran.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestService {

    @Autowired
    UserService userService;

    @Test
    @DisplayName("Integration test")
    void contextLoads() {
        assertFalse(userService.findAll().isEmpty());
        assertEquals("Jone", userService.findAll().get(0).getName());
    }

}
