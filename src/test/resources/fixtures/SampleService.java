package com.example.service;

import com.example.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleService {

    private final OrderRepository orderRepository;

    public SampleService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String getStatus() {
        return "OK";
    }

    public String processOrder(String orderId, int quantity) {
        return orderId + ":" + quantity;
    }

    @Transactional
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }

    public void deleteOrder(String orderId) {
        orderRepository.delete(orderId);
    }
}
