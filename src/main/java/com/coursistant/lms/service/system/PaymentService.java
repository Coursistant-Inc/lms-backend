package com.coursistant.lms.service.system;

import com.coursistant.lms.entity.PaymentRequest;
import net.authorize.Environment;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CreditCardType;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.PaymentType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionResponse;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final String API_LOGIN_ID = "8W6v9dP5"; // 替换成你的
    private static final String TRANSACTION_KEY = "4d65696KPa6wnB6U"; // 替换成你的

    public String chargeCreditCard(PaymentRequest request) {
        // 设置 API 登录凭证和环境
        ApiOperationBase.setEnvironment(Environment.SANDBOX);
        MerchantAuthenticationType merchantAuthenticationType = new MerchantAuthenticationType();
        merchantAuthenticationType.setName(API_LOGIN_ID);
        merchantAuthenticationType.setTransactionKey(TRANSACTION_KEY);
        ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);

        // 创建信用卡信息
        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(request.getCardNumber());
        creditCard.setExpirationDate(request.getExpirationDate());
        creditCard.setCardCode(request.getCardCode());

        PaymentType paymentType = new PaymentType();
        paymentType.setCreditCard(creditCard);

        // 设置交易请求信息
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setAmount(BigDecimal.valueOf(request.getAmount()));
        txnRequest.setPayment(paymentType);

        // 构造交易请求
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setTransactionRequest(txnRequest);

        // 发起交易请求
        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();
        CreateTransactionResponse response = controller.getApiResponse();

        // 判断交易响应
        if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
            if (response.getTransactionResponse() != null && response.getTransactionResponse().getMessages() != null) {
                return "Transaction Success. ID: " + response.getTransactionResponse().getTransId();
            } else if (response.getTransactionResponse() != null && response.getTransactionResponse().getErrors() != null) {
                // 关键修改：使用 TransactionResponse.Errors.Error
                TransactionResponse.Errors.Error error =
                        response.getTransactionResponse().getErrors().getError().get(0);
                System.out.println("Error Code: " + error.getErrorCode());
                System.out.println("Error Text: " + error.getErrorText());
                return "Transaction Failed. Error: " + error.getErrorText();
            }
        } else if (response != null && response.getTransactionResponse() != null && response.getTransactionResponse().getErrors() != null) {
            TransactionResponse.Errors.Error error =
                    response.getTransactionResponse().getErrors().getError().get(0);
            System.out.println("Error Code: " + error.getErrorCode());
            System.out.println("Error Text: " + error.getErrorText());
            return "Transaction Failed. Error: " + error.getErrorText();
        } else {
            return "Transaction Failed. General Error.";
        }

        return "Transaction Failed. General Error.";
    }
}