package com.coursistant.lms.controller.sales;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.SalesRequest;
import com.coursistant.lms.utils.EmailUtil;

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
