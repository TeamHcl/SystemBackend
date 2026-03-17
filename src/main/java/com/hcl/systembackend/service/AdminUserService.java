package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.AdminTransactionHistoryItem;
import com.hcl.systembackend.dto.AnomalousUserView;
import com.hcl.systembackend.dto.AdminUserView;
import com.hcl.systembackend.entity.Admin;
import com.hcl.systembackend.entity.Customer;
import com.hcl.systembackend.repository.AdminRepository;
import com.hcl.systembackend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdminUserService {
    private final CustomerRepository customerRepository;
    private final AdminRepository adminRepository;
    private final AdminTransactionService adminTransactionService;

    public AdminUserService(
            CustomerRepository customerRepository,
            AdminRepository adminRepository,
            AdminTransactionService adminTransactionService
    ) {
        this.customerRepository = customerRepository;
        this.adminRepository = adminRepository;
        this.adminTransactionService = adminTransactionService;
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

    public List<AnomalousUserView> getAnomalousUsers() {
        List<AdminTransactionHistoryItem> flaggedTransactions = adminTransactionService.getAllTransactions().stream()
                .filter(AdminTransactionHistoryItem::flaggedFraud)
                .filter(item -> item.customerId() != null)
                .toList();

        if (flaggedTransactions.isEmpty()) {
            return List.of();
        }

        Map<Integer, Customer> customersById = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getCustomerId, customer -> customer, (left, right) -> left));

        return flaggedTransactions.stream()
                .collect(Collectors.groupingBy(AdminTransactionHistoryItem::customerId))
                .entrySet().stream()
                .map(entry -> toAnomalousUserView(entry.getKey(), entry.getValue(), customersById))
                .sorted(Comparator.comparing(AnomalousUserView::maxRiskScore).reversed())
                .toList();
    }

    private AnomalousUserView toAnomalousUserView(
            Integer customerId,
            List<AdminTransactionHistoryItem> transactions,
            Map<Integer, Customer> customersById
    ) {
        Customer customer = customersById.get(customerId);
        double averageRisk = transactions.stream().mapToDouble(AdminTransactionHistoryItem::anomalyScore).average().orElse(0.0);
        double maxRisk = transactions.stream().mapToDouble(AdminTransactionHistoryItem::anomalyScore).max().orElse(0.0);
        String reason = transactions.stream()
                .map(AdminTransactionHistoryItem::fraudReason)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("transaction behavior deviates from baseline");

        return new AnomalousUserView(
                customerId.longValue(),
                customer == null ? "Unknown User" : customer.getName(),
                customer == null ? "unknown@mockbank.local" : customer.getEmail(),
                transactions.size(),
                round(averageRisk),
                round(maxRisk),
                reason
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
