package com.ningmeng.manage_course.controller;

import com.ningmeng.api.courseapi.CategoryControllerApi;
import com.ningmeng.framework.domain.course.ext.CategoryNode;
import com.ningmeng.manage_course.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/category")
public class CategoryController implements CategoryControllerApi {

    @Autowired
    CategoryService categoryService;

    //分类查询
    @GetMapping("/findCategoryList/{parentId}")
    @Override
    public CategoryNode findCategoryList(@PathVariable("parentId") String parentId) {
        return categoryService.findCategoryList(parentId);
    }
}
