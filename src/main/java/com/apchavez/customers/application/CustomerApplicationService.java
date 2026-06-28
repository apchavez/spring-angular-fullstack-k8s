package com.apchavez.customers.application;

import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.service.CustomerDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CustomerApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerApplicationService.class);

    private final CustomerDomainService domainService;

    public CustomerApplicationService(CustomerDomainService domainService) {
        this.domainService = domainService;
    }

    @Transactional
    public Mono<Customer> createCustomer(Customer customer) {
        log.info("Caso de uso: crear cliente — nombre='{}', apellido='{}'",
                customer.nombre(), customer.apellido());
        return domainService.createCustomer(customer)
                .doOnSuccess(saved -> log.info("Cliente creado — id={}", saved.id()));
    }

    public Mono<Customer> findById(Integer id) {
        log.debug("Caso de uso: buscar cliente — id={}", id);
        return domainService.findById(id);
    }

    public Flux<Customer> listActiveCustomers() {
        log.debug("Caso de uso: listar clientes activos");
        return domainService.listActiveCustomers();
    }

    @Transactional
    public Mono<Customer> updateCustomer(Integer id, Customer updatedData) {
        log.info("Caso de uso: actualizar cliente — id={}", id);
        return domainService.updateCustomer(id, updatedData)
                .doOnSuccess(updated -> log.info("Cliente actualizado — id={}", updated.id()));
    }

    @Transactional
    public Mono<Void> deleteCustomer(Integer id) {
        log.info("Caso de uso: eliminar cliente — id={}", id);
        return domainService.deleteCustomer(id)
                .doOnSuccess(v -> log.info("Cliente eliminado — id={}", id));
    }
}
