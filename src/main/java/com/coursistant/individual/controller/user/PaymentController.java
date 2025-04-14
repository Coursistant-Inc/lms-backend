package com.coursistant.individual.controller.user;

import com.coursistant.individual.entity.PaymentRequest;
import com.coursistant.individual.service.system.PaymentService;
import com.coursistant.individual.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@Api(value = "Payment API", tags = {"Payment Controller"})
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/charge")
    @ApiOperation(value = "Charge Payment", notes = "Processes a credit card payment and returns the transaction result.")
    public Result charge(@RequestBody PaymentRequest request) {
        String message = paymentService.chargeCreditCard(request);
        return Result.success(message);
    }
}