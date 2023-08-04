package cn.zhumingwu.client;

import lombok.extern.slf4j.Slf4j;
import cn.zhumingwu.client.service.DatasetAnnotationService;
import cn.zhumingwu.client.entity.DatasetAnnotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * @author zhumingwu
 * @ClassName:
 * @description:
 * @since 2020-12-09
 **/
@SpringBootTest
@ContextConfiguration(classes = {UserApplication.class})
@Slf4j
public class DatasetAnnotationServiceTests {

    @Autowired
    DatasetAnnotationService service;


    /***
    * 查询所有
    */
    @Test
    public void TestDatasetAnnotationQuery() {
        long count = this.service.findAll("").size();
        Assert.isTrue(count > 0, "查询到数据");
    }
 
    @Test
    public void TestInsertDatasetAnnotation() {
        DatasetAnnotation entity = new DatasetAnnotation();
        entity.setId("test");
        entity.setDatasetId("test");
        entity.setFileId("test");
        entity.setName("test");
        entity.setType("test");
        entity.setIndex(555);
        entity.setTop(555L);
        entity.setLeft(555L);
        entity.setWidth(555);
        entity.setHeight(555);
        // todo:Boolean ;
        // todo:Boolean ;
        entity.setCid(555);
        entity.setCreatedTime(new Date());
        entity.setUpdatedTime(new Date());
        entity.setCreatedBy(555L);
        entity.setUpdatedBy(555L);
        // todo:Status ;
        int count = this.service.insert(entity);
        Assert.isTrue(count > 0, "插件数据");
    }


//        /***
//        * 保存
//        */
//    @Test
//    @Rollback
//    @Transactional
//    public void TestEditDatasetAnnotation() {

//        boolean flag = this.service.saveOrUpdateDatasetAnnotation(DatasetAnnotationSaveReq);
//        Assert.assertTrue(flag);
//    }


//    /***
//    * 删除
//    */
//    @Test
//    @Rollback
//    @Transactional
//    public void TestDeleteDatasetAnnotation() {

////boolean flag = this.service.deleteDatasetAnnotation(DatasetAnnotationDelReq);

//        Assert.assertTrue(flag);
//    }



//    /***
//    * 查询所有
//    */
//    @Test
//    public void TestSelect() {
//        List<DatasetAnnotation> DatasetAnnotationList = this.service.list(null);
//        if (DatasetAnnotationList != null) {
//            DatasetAnnotationList.forEach(System.out::println);
//        }
//    }
}
