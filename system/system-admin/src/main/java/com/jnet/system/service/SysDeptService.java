package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.system.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> getDeptTree();

    List<SysDept> getDeptList();

    SysDept getDeptById(Long deptId);

    Boolean addDept(SysDept dept);

    Boolean updateDept(SysDept dept);

    Boolean deleteDept(Long deptId);

    List<SysDept> getDeptsByParentId(Long parentId);

}
