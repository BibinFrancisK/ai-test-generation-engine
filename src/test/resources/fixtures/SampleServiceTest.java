package com.example.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private SampleService sampleService;

    @Test
    @DisplayName("getStatus returns OK when service is healthy")
    void getStatusReturnsOk() {
        String status = sampleService.getStatus();
        assertThat(status).isEqualTo("OK");
    }

    @Test
    @DisplayName("deleteOrder delegates deletion to the repository")
    void deleteOrderDelegatesToRepository() {
        sampleService.deleteOrder("ord-001");
        verify(orderRepository).delete("ord-001");
    }
}
