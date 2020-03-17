package com.ningmeng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.netflix.discovery.converters.Auto;
import com.ningmeng.framework.domain.cms.CmsPage;
import com.ningmeng.framework.domain.cms.CmsSite;
import com.ningmeng.framework.domain.cms.request.QueryPageRequest;
import com.ningmeng.framework.domain.cms.response.CmsCode;
import com.ningmeng.framework.domain.cms.response.CmsPageResult;
import com.ningmeng.framework.domain.cms.response.CmsPostPageResult;
import com.ningmeng.framework.domain.course.response.CoursePublishResult;
import com.ningmeng.framework.exception.CustomExceptionCast;
import com.ningmeng.framework.model.response.*;
import com.ningmeng.manage_cms.config.RabbitmqConfig;
import com.ningmeng.manage_cms.dao.CmsPageRepository;
import com.ningmeng.manage_cms.dao.CmsSiteRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Lenovo on 2020/2/11.
 */
@Service
public class CmsPageService {
    @Autowired
    CmsPageRepository cmsPageRepository;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    CmsSiteRepository cmsSiteRepository;

    //分页查询

    /**
     *
     * @param page
     * @param size
     * @param queryPageRequest 自定义页面属性  查询条件
     * @return
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {
        //条件匹配器
        ExampleMatcher exampleMatcher = ExampleMatcher.matching();
        exampleMatcher = exampleMatcher.withMatcher("pageAlise", ExampleMatcher.GenericPropertyMatchers.contains());
        //条件值
        CmsPage cmsPage = new CmsPage();
        //站点ID
       if (!StringUtils.isEmpty(queryPageRequest.getSiteId())){
           cmsPage.setSiteId(queryPageRequest.getSiteId());
       }
       //页面别名
        if (!StringUtils.isEmpty(queryPageRequest.getPageAliase())){
           cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        //创建条件实例
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);

        if (queryPageRequest == null) {
            queryPageRequest = new QueryPageRequest();
        }
        if (page <= 0) {
            page = 1;
        }
        page = page - 1;
        if (size <= 0) {
            size = 20;
        }
        //分页对象
       Pageable pageable= PageRequest.of(page,size);
        //分页查询
      /*  Page<CmsPage> all = cmsPageRepository.findAll(pageable);*/
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);
        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());
        return new QueryResponseResult(CommonCode.SUCCESS,cmsPageQueryResult);
    }
    //添加页面+异常处理
    public CmsPageResult add(CmsPage cmsPage) {
        //判断页面是否存在，根据站点id,页面名称，页面webpath
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (cmsPage1 != null) {
            //抛出异常 CMS_ADDPAGE_EXISTSNAME
            CustomExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        //页面主键id由spring data自动生成
        cmsPage.setPageId(null);
        cmsPageRepository.save(cmsPage);
        //返回结果
        CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, cmsPage);
        return  cmsPageResult;

    }
    //根据id查询页面
    public CmsPage getById(String id){
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if(optional.isPresent()){
            System.out.println("--------------:"+optional.get());
            return  optional.get();
        }
        return  null;
    }
    //更新页面信息
    public CmsPageResult update(String id,CmsPage cmsPage){
        //根据id查询信息（this?）
        CmsPage one = this.getById(id);
        if(one!=null){
            //更新数据
            one.setTemplateId(cmsPage.getTemplateId());
            one.setSiteId(cmsPage.getSiteId());
            one.setPageAliase(cmsPage.getPageAliase());
            one.setPageName(cmsPage.getPageName());
            one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            one.setPageWebPath(cmsPage.getPageWebPath());
            one.setPageAliase(cmsPage.getPageAliase());
            CmsPage save = cmsPageRepository.save(one);
            if (save!=null){
                //返回成功
                return  new CmsPageResult(CommonCode.SUCCESS,save);
            }
        }
        //返回失败
        return  new CmsPageResult(CommonCode.FAIL,null);
    }
    //删除
    public ResponseResult delete(String id){
        CmsPage one = this.getById(id);
        if (one!=null){
            cmsPageRepository.deleteById(id);
            return  new ResponseResult(CommonCode.SUCCESS);
        }
        return  new ResponseResult(CommonCode.FAIL);
    }


    //将页面html保存到页面物理路径
    public ResponseResult postPage(String pageId){
        sendPostPage(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 创建静态页面
     * @param pageId
     */
    private void sendPostPage(String pageId) {
        System.out.println("执行页面静态化程序，保存静态化文件完成。。。。。");
        CmsPage cmsPage= this.getById(pageId);
        if(cmsPage == null){
            CustomExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }

        Map<String,String> msgMap=new HashMap<>();
        msgMap.put("pageId",pageId);
        //消息内容
        String msg = JSON.toJSONString(msgMap);
        //获取站点id作为routingKey
        String siteId= cmsPage.getSiteId();
        //发布消息
        this.rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,siteId, msg);
    }

    public String preview(String cmsPageId) {
        //模板Id
        Optional<CmsPage> cmsPageOptional = cmsPageRepository.findById(cmsPageId);
        if(!cmsPageOptional.isPresent()){
            CustomExceptionCast.cast(CommonCode.SERVER_ERROR);
        }
        CmsPage cmsPage = cmsPageOptional.get();
        //根据cmsPage.getTemplateId()获得GridFS中保存模板文件
        //获得静态化模型数据
        //生成静态化文件内容
        return "";

    }

    //一键发布页面
    public CmsPostPageResult postPageQuick(CmsPage cmsPage){
        //添加页面
        CmsPageResult save = this.add(cmsPage);
        if(!save.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        CmsPage cmsPage1 = save.getCmsPage();
        //要布的页面id
        String pageId = cmsPage1.getPageId();
        //发布页面
        ResponseResult responseResult = this.postPage(pageId);
        if(!responseResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //得到页面的url
        // 页面url=站点域名+站点webpath+页面webpath+页面名称
        // 站点id
        String siteId = cmsPage1.getSiteId();
        //查询站点信息
        CmsSite cmsSite = findCmsSiteById(siteId);
        //站点域名
        String siteDomain = cmsSite.getSiteDomain();
        //站点web路径
        String siteWebPath = cmsSite.getSiteWebPath();
        //页面web路径
        String pageWebPath = cmsPage1.getPageWebPath();
        //页面名称
        String pageName = cmsPage1.getPageName();
        //页面的web访问地址
        String pageUrl = siteDomain+siteWebPath+pageWebPath+pageName;
        return new CmsPostPageResult(CommonCode.SUCCESS,pageUrl);
    }
    //根据id查询站点信息
    public CmsSite findCmsSiteById(String siteId){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }
}
