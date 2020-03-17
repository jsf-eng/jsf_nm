package com.ningmeng.manage_course.service;

import com.ningmeng.framework.domain.course.ext.CategoryNode;
import com.ningmeng.framework.domain.system.SysDictionary;
import com.ningmeng.manage_course.dao.CategoryMapper;
import com.ningmeng.manage_course.dao.SysDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    //分类查询
    public CategoryNode findCategoryList(String parentId) {
        return categoryMapper.findCategoryList(parentId);
    }

}

