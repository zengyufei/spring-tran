package com.example.tran.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User2 {
    private Long id;
    private String name;
    private Integer age;
    private String email;
}
