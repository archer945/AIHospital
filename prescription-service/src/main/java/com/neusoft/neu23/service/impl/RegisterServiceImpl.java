package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.Register;
import com.neusoft.neu23.mapper.RegisterMapper;
import com.neusoft.neu23.service.RegisterService;
import org.springframework.stereotype.Service;

@Service
public class RegisterServiceImpl extends ServiceImpl<RegisterMapper, Register> implements RegisterService {
}

