package cn.zhumingwu.client.service;

import cn.zhumingwu.client.entity.Category;
import cn.zhumingwu.client.repository.CategoryRepository;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {
    @Resource
    CategoryRepository repos;

    public List<Category> findAll(String where, Object... param) {
        return this.repos.query(where,param);
    }

    public int insert(Category entity) {
        return this.repos.insert(entity);
    }

    public int deleteById(Integer id) {
        return this.repos.delete(id);
    }

    public int update(Category entity) {
        return this.repos.update(entity);
    }
}
