package com.piggsoft.webchat.service;

import com.piggsoft.webchat.entity.Order;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

public interface BusinessDataService {

    @Tool(description = "根据参数查询订单信息。可传入orderNo, customer, createAt")
    List<Order> queryOrders(Order queryOrder);

    List<Order> queryAllOrders();
}
