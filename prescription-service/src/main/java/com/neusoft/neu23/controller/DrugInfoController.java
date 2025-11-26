package com.neusoft.neu23.controller;

import com.neusoft.neu23.entity.DrugInfo;
import com.neusoft.neu23.service.DrugInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drug-info")
@CrossOrigin
@Slf4j
@Validated
public class DrugInfoController {

    @Autowired
    private DrugInfoService drugInfoService;

    /**
     * 根据药品ID查询药品信息
     */
    @GetMapping("/{drugId}")
    public ResponseEntity<DrugInfo> getDrugInfo(@PathVariable Long drugId) {
        try {
            DrugInfo drugInfo = drugInfoService.getById(drugId);
            if (drugInfo == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(drugInfo);
        } catch (Exception e) {
            log.error("查询药品信息失败，药品ID: {}", drugId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 按药品名称搜索药品
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDrugByName(@RequestParam(value = "name", required = false) String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "请输入药品名称", "records", List.of()));
            }

            LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(DrugInfo::getName, name.trim());
            wrapper.last("LIMIT 20");

            List<DrugInfo> drugList = drugInfoService.list(wrapper);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "records", drugList,
                    "total", drugList.size()
            ));
        } catch (Exception e) {
            log.error("搜索药品失败，名称: {}", name, e);
            return ResponseEntity.ok(Map.of("success", false, "message", "搜索失败", "records", List.of()));
        }
    }
}

