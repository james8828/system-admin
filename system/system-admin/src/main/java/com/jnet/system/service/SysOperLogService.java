package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysOperLog;

public interface SysOperLogService extends IService<SysOperLog> {

    PageResult<SysOperLog> pageOperLog(PageQuery pageQuery, SysOperLog operLog);

    SysOperLog getOperLogById(Long operId);

    Boolean addOperLog(SysOperLog operLog);

    Boolean deleteOperLog(Long operId);

    Boolean deleteOperLogsByDate(String startDate, String endDate);

}
