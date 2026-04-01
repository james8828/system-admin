package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.system.entity.SysDept;
import com.jnet.system.mapper.SysDeptMapper;
import com.jnet.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysDeptMapper sysDeptMapper;

    @Override
    public List<SysDept> getDeptTree() {
        List<SysDept> depts = list();
        Map<Long, SysDept> deptMap = depts.stream().collect(Collectors.toMap(SysDept::getDeptId, d -> d));
        
        List<SysDept> tree = new ArrayList<>();
        for (SysDept dept : depts) {
            if (dept.getParentId() == 0) {
                tree.add(dept);
            } else {
                SysDept parent = deptMap.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                }
            }
        }
        return tree;
    }

    @Override
    public List<SysDept> getDeptList() {
        return list();
    }

    @Override
    public SysDept getDeptById(Long deptId) {
        return getById(deptId);
    }

    @Override
    public Boolean addDept(SysDept dept) {
        return save(dept);
    }

    @Override
    public Boolean updateDept(SysDept dept) {
        return updateById(dept);
    }

    @Override
    public Boolean deleteDept(Long deptId) {
        return removeById(deptId);
    }

    @Override
    public List<SysDept> getDeptsByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, parentId));
    }

}
