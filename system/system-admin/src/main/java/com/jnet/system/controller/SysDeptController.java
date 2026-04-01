package com.jnet.system.controller;

import com.jnet.system.entity.SysDept;
import com.jnet.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService sysDeptService;

    @GetMapping("/tree")
    public List<SysDept> getDeptTree() {
        return sysDeptService.getDeptTree();
    }

    @GetMapping("/list")
    public List<SysDept> getDeptList() {
        return sysDeptService.getDeptList();
    }

    @GetMapping("/{deptId}")
    public SysDept getDeptById(@PathVariable Long deptId) {
        return sysDeptService.getDeptById(deptId);
    }

    @PostMapping
    public Boolean addDept(@RequestBody SysDept dept) {
        return sysDeptService.addDept(dept);
    }

    @PutMapping
    public Boolean updateDept(@RequestBody SysDept dept) {
        return sysDeptService.updateDept(dept);
    }

    @DeleteMapping("/{deptId}")
    public Boolean deleteDept(@PathVariable Long deptId) {
        return sysDeptService.deleteDept(deptId);
    }

}
