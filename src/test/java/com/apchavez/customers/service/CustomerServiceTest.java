package com.apchavez.customers.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apchavez.customers.model.Customer;
import com.apchavez.customers.repository.CustomerRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerService service;

    @Test
    void createCustomer_ShouldSave_WhenIsNew() {
        Customer customer = new Customer(1, "Alex", "Prieto", "active", 30);
        when(repository.findById(1)).thenReturn(Mono.empty());
        when(repository.save(any(Customer.class))).thenReturn(Mono.just(customer));
        StepVerifier.create(service.createCustomer(customer))
                .expectNextMatches(a -> a.name().equals("Alex"))
                .verifyComplete();
    }

    @Test
    void createCustomer_ShouldThrowError_WhenIdExists() {
        Customer customer = new Customer(1, "Alex", "Prieto", "active", 30);
        when(repository.findById(1)).thenReturn(Mono.just(customer));
        StepVerifier.create(service.createCustomer(customer))
                .expectErrorMessage("The ID already exists")
                .verify();
    }
}