package cn.zhumingwu.client.controller;

import cn.zhumingwu.base.model.Result;
import cn.zhumingwu.client.entity.ConnectProvider;
import cn.zhumingwu.client.service.ConnectProviderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("connectprovider")
@Slf4j
public class ConnectProviderController extends BaseController {
    @Autowired
    private ConnectProviderService service;

    @GetMapping(value = "/list")
    public Result<Object> list() {
        return Result.success(this.service.findAll(""));
    }

    @PostMapping(value = "/insert")
    public Result<Object> insert(@RequestBody ConnectProvider entity) {
        try {
            return Result.success(this.service.insert(entity));
        } catch (Exception ex) {
            return Result.error(ex);
        }
    }

    @GetMapping("/delete/{id}")
    public Result<Object> delete(@PathVariable(value = "id") Integer id) {
        this.service.deleteById(id);
        return Result.success();
    }

    @GetMapping(value = "/update")
    public Result<Object> update(@RequestBody ConnectProvider entity) {
        try {
            return Result.success(this.service.update(entity));
        } catch (Exception ex) {
            return Result.error(ex);
        }
    }

}
