package com.jnet.system.controller;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysOperLog;
import com.jnet.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/operLog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService sysOperLogService;

    @GetMapping("/page")
    public PageResult<SysOperLog> pageOperLog(PageQuery pageQuery, SysOperLog operLog) {
        return sysOperLogService.pageOperLog(pageQuery, operLog);
    }

    @GetMapping("/{operId}")
    public SysOperLog getOperLogById(@PathVariable Long operId) {
        return sysOperLogService.getOperLogById(operId);
    }

    @PostMapping
    public Boolean addOperLog(@RequestBody SysOperLog operLog) {
        return sysOperLogService.addOperLog(operLog);
    }

    @DeleteMapping("/{operId}")
    public Boolean deleteOperLog(@PathVariable Long operId) {
        return sysOperLogService.deleteOperLog(operId);
    }

}
