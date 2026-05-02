package com.piggsoft.webchat.service;

import com.piggsoft.webchat.entity.Order;
import com.piggsoft.webchat.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessDataServiceImpl implements BusinessDataService {

    private final OrderMapper orderMapper;

    @Override
    public List<Order> queryOrders(Order queryOrder) {


        return orderMapper.query(queryOrder);
    }

    @Override
    public List<Order> queryAllOrders() {
        return orderMapper.selectAll();
    }
}
