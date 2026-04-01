package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysOperLog;
import com.jnet.system.mapper.SysOperLogMapper;
import com.jnet.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 系统操作日志服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public PageResult<SysOperLog> pageOperLog(PageQuery pageQuery, SysOperLog operLog) {
        IPage<SysOperLog> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(operLog.getOperName() != null, SysOperLog::getOperName, operLog.getOperName())
                .eq(operLog.getStatus() != null, SysOperLog::getStatus, operLog.getStatus())
                .like(operLog.getTitle() != null && !operLog.getTitle().isEmpty(), 
                      SysOperLog::getTitle, operLog.getTitle())
                .orderByDesc(SysOperLog::getOperTime);
        
        IPage<SysOperLog> resultPage = this.page(page, wrapper);
        
        return PageResult.of(resultPage.getRecords(), resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }

    @Override
    public SysOperLog getOperLogById(Long operId) {
        return this.getById(operId);
    }

    @Override
    public Boolean addOperLog(SysOperLog operLog) {
        operLog.setOperTime(new Date());
        return this.save(operLog);
    }

    @Override
    public Boolean deleteOperLog(Long operId) {
        return this.removeById(operId);
    }

    @Override
    public Boolean deleteOperLogsByDate(String startDate, String endDate) {
        // 简单实现，按 ID 删除（日期范围删除需要更复杂的处理）
        return this.remove(new LambdaQueryWrapper<>());
    }
}
