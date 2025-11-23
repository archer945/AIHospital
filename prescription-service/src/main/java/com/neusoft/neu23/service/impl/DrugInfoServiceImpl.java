package com.neusoft.neu23.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.entity.DrugInfo;
import com.neusoft.neu23.mapper.DrugInfoMapper;
import com.neusoft.neu23.service.DrugInfoService;
import org.springframework.stereotype.Service;

@Service
public class DrugInfoServiceImpl extends ServiceImpl<DrugInfoMapper, DrugInfo> implements DrugInfoService {
}

