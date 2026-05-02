package com.piggsoft.webchat.service.springai;

import com.piggsoft.webchat.entity.Order;
import com.piggsoft.webchat.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

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
