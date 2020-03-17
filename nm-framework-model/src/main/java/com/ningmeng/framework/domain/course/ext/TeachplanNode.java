package com.ningmeng.framework.domain.course.ext;

import com.ningmeng.framework.domain.course.Teachplan;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 课程计划的自定义类    针对于elementUI中Three数据结构
 */
@Data
@ToString
public class TeachplanNode extends Teachplan {

    List<TeachplanNode> children;

    //媒资信息
    private String mediaId;
    private String mediaFileOriginalName;
}
