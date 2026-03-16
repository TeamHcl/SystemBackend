package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.AdminUserView;
import com.hcl.systembackend.entity.Admin;
import com.hcl.systembackend.entity.Customer;
import com.hcl.systembackend.repository.AdminRepository;
import com.hcl.systembackend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AdminUserService {
    private final CustomerRepository customerRepository;
    private final AdminRepository adminRepository;

    public AdminUserService(CustomerRepository customerRepository, AdminRepository adminRepository) {
        this.customerRepository = customerRepository;
        this.adminRepository = adminRepository;
    }

    public List<AdminUserView> getAllUsers() {
        Stream<AdminUserView> admins = adminRepository.findAll().stream()
                .map(this::toAdminView);
        Stream<AdminUserView> customers = customerRepository.findAll().stream()
                .map(this::toCustomerView);

        return Stream.concat(admins, customers)
                .sorted(Comparator.comparing(AdminUserView::role).thenComparing(AdminUserView::id))
                .toList();
    }

    private AdminUserView toAdminView(Admin admin) {
        return new AdminUserView(admin.getAdminId().longValue(), admin.getName(), admin.getEmail(), "ADMIN");
    }

    private AdminUserView toCustomerView(Customer customer) {
        return new AdminUserView(customer.getCustomerId().longValue(), customer.getName(), customer.getEmail(), "USER");
    }
}
