package com.ningmeng.manage_course.dao;

import com.ningmeng.framework.domain.course.CourseBase;
import com.ningmeng.framework.domain.system.SysDictionary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestDao {
    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    SysDictionaryRepository sysDicthinaryRepository;

    @Autowired
    CourseMapper courseMapper;

    @Test
    public void testCourseBaseRepository(){
        Optional<CourseBase> optional = courseBaseRepository.findById("402885816240d276016240f7e5000002");
        if(optional.isPresent()){
            CourseBase courseBase = optional.get();
            System.out.println(courseBase);
        }

    }

    @Test
    public void testCourseMapper(){
        CourseBase courseBase = courseMapper.findCourseBaseById("402885816240d276016240f7e5000002");
        System.out.println(courseBase);

    }

    @Test
    public void findByDType(){
        SysDictionary sysDictionary = sysDicthinaryRepository.findById("5a7d50bdd019f150f4ab8ef7").get();
        System.out.println("-------------------------");
        System.out.println(sysDictionary);
    }
}
