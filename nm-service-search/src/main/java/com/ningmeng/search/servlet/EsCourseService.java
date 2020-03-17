package com.ningmeng.search.servlet;

import com.ningmeng.framework.domain.course.CoursePub;
import com.ningmeng.framework.domain.course.TeachplanMediaPub;
import com.ningmeng.framework.domain.search.CourseSearchParam;
import com.ningmeng.framework.model.response.CommonCode;
import com.ningmeng.framework.model.response.QueryResponseResult;
import com.ningmeng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsCourseService {


    @Value("${ningmeng.course.index}")
    private String es_index;

    @Value("${ningmeng.course.media_index}")
    private String media_index;

    @Value("${ningmeng.course.type}")
    private String ex_type;

    @Value("${ningmeng.course.media_type}")
    private String media_type;

    @Value("${ningmeng.course.sourcefield}")
    private String sourcefield;

    @Value("${ningmeng.course.media_sourcefield}")
    private String media_sourcefield;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public QueryResponseResult getmedia(String[] teachplanIds) {
        //设置索引库
        SearchRequest searchRequest = new SearchRequest(media_index);
        searchRequest.types(media_type);
        //创建查询条件对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //绑定查询
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id",teachplanIds));

        //source源字段过虑
        /*String[] source_fields = media_sourcefield.split(",");
        searchSourceBuilder.fetchSource(source_fields, new String[]{});*/

        //请求搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest);
        }catch (Exception e){
            e.printStackTrace();
        }
        //获取搜索结果
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Map<String,CoursePub> map = new HashMap<>();
        //数据列表
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            TeachplanMediaPub teachplanMediaPub =new TeachplanMediaPub();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            //取出课程计划媒资信息
            String courseid = (String) sourceAsMap.get("courseid");
            String media_id = (String) sourceAsMap.get("media_id");
            String media_url = (String) sourceAsMap.get("media_url");
            String teachplan_id = (String) sourceAsMap.get("teachplan_id");
            String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");
            teachplanMediaPub.setCourseId(courseid);
            teachplanMediaPub.setMediaUrl(media_url);
            teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
            teachplanMediaPub.setMediaId(media_id);
            teachplanMediaPub.setTeachplanId(teachplan_id);
            //将数据加入列表
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        //构建返回课程媒资信息对象
        QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();
        queryResult.setList(teachplanMediaPubList);
        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;
    }

    public Map<String, CoursePub> getall(String id) {
        //设置索引库
        SearchRequest searchRequest = new SearchRequest(es_index);
        searchRequest.types(ex_type);
        //创建查询条件对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //绑定查询
        searchSourceBuilder.query(QueryBuilders.termQuery("id",id));
        //请求搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest);
        }catch (Exception e){
            e.printStackTrace();
        }
        SearchHits searchHits = searchResponse.getHits();
        SearchHit[] hits = searchHits.getHits();
        Map<String,CoursePub> map = new HashMap<>();
        for (SearchHit hit : searchHits) {
            String courseId = hit.getId();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String courseIds = (String) sourceAsMap.get("id");
            String name = (String) sourceAsMap.get("name");
            String grade = (String) sourceAsMap.get("grade");
            String charge = (String) sourceAsMap.get("charge");
            String pic = (String) sourceAsMap.get("pic");
            String description = (String) sourceAsMap.get("description");
            String teachplan = (String) sourceAsMap.get("teachplan");
            CoursePub coursePub = new CoursePub();
            coursePub.setId(courseIds);
            coursePub.setName(name);
            coursePub.setPic(pic);
            coursePub.setGrade(grade);
            coursePub.setTeachplan(teachplan);
            coursePub.setDescription(description);
            map.put(courseId,coursePub);
        }
        return map;
    }

    /**
     * 关键字查询
     * @param page
     * @param size
     * @param courseSearchParam
     * @return
     */
    public QueryResponseResult list(int page, int size, CourseSearchParam courseSearchParam){

        if(page<=0){
            page = 1;
        }
        if(size<=0){
            size=10;
        }

        //创建查询请求
        SearchRequest searchRequest = new SearchRequest(es_index);
        searchRequest.types(ex_type);
        //创建查询条件对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置分页的起始页码
        searchSourceBuilder.from(page);
        //设置分页的每页显示数
        searchSourceBuilder.size(size);

        //维护字段
        String[] str = sourcefield.split(",");
        searchSourceBuilder.fetchSource(str,new String[]{});

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if(StringUtils.isNotEmpty(courseSearchParam.getKeyword())) {
            //匹配关键字  参数 1.要查询的值 2.想要查询的字段
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(), "name", "teachplan", "description");
            multiMatchQueryBuilder.minimumShouldMatch("80%");
            multiMatchQueryBuilder.field("name", 10);

            //Boolean方式查询（filter过滤器）
            boolQueryBuilder.must(multiMatchQueryBuilder);

        }
        //一级分类
        if(StringUtils.isNotEmpty(courseSearchParam.getMt())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt",courseSearchParam.getMt()));
        }
        //二级分类
        if(StringUtils.isNotEmpty(courseSearchParam.getSt())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("st",courseSearchParam.getSt()));
        }
        //等级查询
        if(StringUtils.isNotEmpty(courseSearchParam.getGrade())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade",courseSearchParam.getGrade()));
        }

        //高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        //配置高亮显示字段
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        //查询条件绑定高亮查询
        searchSourceBuilder.highlighter(highlightBuilder);

        //绑定查询
        searchSourceBuilder.query(boolQueryBuilder);
        //请求搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try{
            //执行搜索
            searchResponse = restHighLevelClient.search(searchRequest);
        }catch (Exception e){
        
        }
        SearchHits searchHits = searchResponse.getHits();
        long total = searchHits.getTotalHits();
        SearchHit[] hits = searchHits.getHits();
        //数据列表
        ArrayList<CoursePub> list = new ArrayList<>();
        for(SearchHit hit:hits){
            CoursePub coursePub = new CoursePub();
            //取出course
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            //取出名称
            String name = (String) sourceAsMap.get("name");

            Map<String, HighlightField> map = hit.getHighlightFields();
            if(map!=null){
                HighlightField highlightField = map.get("name");
                if(highlightField!=null){
                    Text[] texts = highlightField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for(Text text:texts){
                        stringBuffer.append(text.string());
                    }
                    //如果有对应的高亮显示的值 就执行替换
                    name = stringBuffer.toString();
                }
            }
            coursePub.setName(name);
            //图片
            String pic = (String) sourceAsMap.get("pic");
            coursePub.setPic(pic);
            //价格
            Float price = null;
            try {
                if(sourceAsMap.get("price")!=null ){
                    price = Float.parseFloat((String) sourceAsMap.get("price"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            coursePub.setPrice(price);
            Float price_old = null;
            try {
                if(sourceAsMap.get("price_old")!=null ){
                    price_old = Float.parseFloat((String) sourceAsMap.get("price_old"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            coursePub.setPrice_old(price_old);
            list.add(coursePub);
        }
        QueryResult<CoursePub> queryResult = new QueryResult<>();
        queryResult.setList(list);
        queryResult.setTotal(total);
        QueryResponseResult coursePubQueryResponseResult = new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return coursePubQueryResponseResult;
    }
}
