package com.example.tran.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.tran.entity.User2;
import com.example.tran.mapper.User2Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User service impl.
 */
@Service
@RequiredArgsConstructor
public class User2Service extends ServiceImpl<User2Mapper, User2> implements IService<User2> {

    /**
     * user dao.
     */
    private final User2Mapper user2Mapper;


    public List<User2> findAll() {
        return user2Mapper.selectList(Wrappers.emptyWrapper());
    }
}
