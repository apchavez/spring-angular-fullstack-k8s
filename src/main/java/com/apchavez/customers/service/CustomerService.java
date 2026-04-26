package com.apchavez.customers.service;

import org.springframework.stereotype.Service;

import com.apchavez.customers.model.Customer;
import com.apchavez.customers.repository.CustomerRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Mono<Customer> createCustomer(Customer customer) {
        if (customer.id() == null) {
            return repository.save(customer);
        }

        return repository.findById(customer.id())
                .flatMap(existente -> Mono.<Customer>error(new RuntimeException("The ID already exists")))
                .switchIfEmpty(repository.save(customer));
    }

    public Flux<Customer> listActiveCustomers() {
        return repository.findAll().filter(a -> "active".equalsIgnoreCase(a.state()));
    }
}