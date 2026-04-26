package com.apchavez.customers.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.apchavez.customers.model.Customer;
import com.apchavez.customers.service.CustomerService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Customer>> createCustomer(@Valid @RequestBody Customer customer) {
        return service.createCustomer(customer)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/active")
    public Flux<Customer> listActiveCustomers() {
        return service.listActiveCustomers();
    }
}