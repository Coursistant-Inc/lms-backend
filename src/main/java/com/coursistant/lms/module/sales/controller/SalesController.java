package com.coursistant.lms.module.sales.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.sales.entity.SalesRequest;
import com.coursistant.lms.shared.util.EmailUtil;

import jakarta.annotation.Resource;

@Resource
@RestController
public class SalesController {

    @Resource
    private EmailUtil emailUtil;


    @PostMapping("/sales")
    public Result sendSalesRequest(@RequestBody SalesRequest salesRequest)
    {
        emailUtil.sendSalesRequest(salesRequest);
        return Result.success();

    }

}
