package com.jnet.system.api.client;

import com.jnet.common.result.Result;
import com.jnet.system.api.dto.SysDeptDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门服务 Feign 客户端
 */
@FeignClient(name = "jnet-system-admin", contextId = "sysDeptClient")
public interface SysDeptFeignClient {

    /**
     * 获取部门树
     */
    @GetMapping("/api/system/dept/tree")
    Result<List<SysDeptDTO>> getDeptTree();

    /**
     * 根据 ID 查询部门
     */
    @GetMapping("/api/system/dept/{deptId}")
    Result<SysDeptDTO> getDeptById(@PathVariable("deptId") Long deptId);

    /**
     * 新增部门
     */
    @PostMapping("/api/system/dept")
    Result<Boolean> addDept(@RequestBody SysDeptDTO dept);

    /**
     * 修改部门
     */
    @PutMapping("/api/system/dept")
    Result<Boolean> updateDept(@RequestBody SysDeptDTO dept);

    /**
     * 删除部门
     */
    @DeleteMapping("/api/system/dept/{deptId}")
    Result<Boolean> deleteDept(@PathVariable("deptId") Long deptId);
}
