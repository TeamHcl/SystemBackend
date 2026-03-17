package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.MockBankHistorySyncResult;
import com.hcl.systembackend.dto.SigninRequest;
import com.hcl.systembackend.entity.Account;
import com.hcl.systembackend.entity.Customer;
import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.repository.AccountRepository;
import com.hcl.systembackend.repository.CustomerRepository;
import com.hcl.systembackend.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MockBankHistorySyncService {
    private static final int DEFAULT_PAGE_SIZE = 500;

    private final RestClient restClient;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final String syncAdminEmail;
    private final String syncAdminPassword;
    private final int syncPageSize;

    public MockBankHistorySyncService(
            @Value("${mockbank.base-url:http://localhost:8080}") String mockBankBaseUrl,
            @Value("${mockbank.sync.admin-email:admin@example.com}") String syncAdminEmail,
            @Value("${mockbank.sync.admin-password:secret123}") String syncAdminPassword,
            @Value("${mockbank.sync.page-size:500}") int syncPageSize,
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(mockBankBaseUrl))
                .build();
        this.syncAdminEmail = syncAdminEmail;
        this.syncAdminPassword = syncAdminPassword;
        this.syncPageSize = syncPageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(syncPageSize, 1000);
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public MockBankHistorySyncResult syncHistory(LocalDateTime since) {
        String adminToken = authenticateWithMockBank();

        List<RemoteUser> remoteUsers = fetchUsers(adminToken);
        SyncCustomerResult customerResult = upsertCustomers(remoteUsers);

        List<RemoteAccount> remoteAccounts = fetchAccounts(adminToken);
        SyncAccountResult accountResult = upsertAccounts(remoteAccounts, customerResult.customersBySourceUserId());

        SyncTransactionResult transactionResult = upsertTransactions(
                adminToken,
                since,
                accountResult.accountsBySourceAccountId()
        );

        return new MockBankHistorySyncResult(
                customerResult.syncedCount(),
                accountResult.syncedCount(),
                transactionResult.insertedCount(),
                transactionResult.updatedCount(),
                transactionResult.skippedCount(),
                transactionResult.pagesFetched(),
                LocalDateTime.now(),
                "MockBank history synchronized successfully"
        );
    }

    private String authenticateWithMockBank() {
        try {
            RemoteAdminAuthResponse response = restClient.post()
                    .uri("/api/auth/admin/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SigninRequest(syncAdminEmail, syncAdminPassword))
                    .retrieve()
                    .body(RemoteAdminAuthResponse.class);

            if (response == null || isBlank(response.token())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MockBank admin authentication failed for sync");
            }
            return response.token();
        } catch (RestClientResponseException exception) {
            throw mapRemoteException(exception, "MockBank admin authentication failed for sync");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MockBank service is unavailable", exception);
        }
    }

    private List<RemoteUser> fetchUsers(String adminToken) {
        try {
            List<RemoteUser> users = restClient.get()
                    .uri("/api/admin/export/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteUser>>() {
                    });
            return users == null ? List.of() : users;
        } catch (RestClientResponseException exception) {
            throw mapRemoteException(exception, "MockBank users export failed");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MockBank service is unavailable", exception);
        }
    }

    private List<RemoteAccount> fetchAccounts(String adminToken) {
        try {
            List<RemoteAccount> accounts = restClient.get()
                    .uri("/api/admin/export/accounts")
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteAccount>>() {
                    });
            return accounts == null ? List.of() : accounts;
        } catch (RestClientResponseException exception) {
            throw mapRemoteException(exception, "MockBank accounts export failed");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MockBank service is unavailable", exception);
        }
    }

    private SyncCustomerResult upsertCustomers(List<RemoteUser> remoteUsers) {
        Map<Long, Customer> customersBySourceUserId = new HashMap<>();
        int syncedCount = 0;

        for (RemoteUser remoteUser : remoteUsers) {
            if (remoteUser == null || remoteUser.id() == null) {
                continue;
            }

            Customer customer = customerRepository.findBySourceUserId(remoteUser.id())
                    .orElseGet(Customer::new);

            customer.setSourceUserId(remoteUser.id());
            customer.setName(defaultValue(remoteUser.name(), "MockBank User " + remoteUser.id()));
            customer.setEmail(defaultValue(remoteUser.email(), "user" + remoteUser.id() + "@mockbank.local"));
            customer.setPhone(defaultValue(customer.getPhone(), "9999999999"));
            customer.setAddress(defaultValue(customer.getAddress(), "Synced from MockBank"));
            if (customer.getCreatedAt() == null) {
                customer.setCreatedAt(Timestamp.from(Instant.now()));
            }

            Customer saved = customerRepository.save(customer);
            customersBySourceUserId.put(remoteUser.id(), saved);
            syncedCount++;
        }

        return new SyncCustomerResult(customersBySourceUserId, syncedCount);
    }

    private SyncAccountResult upsertAccounts(List<RemoteAccount> remoteAccounts, Map<Long, Customer> customersBySourceUserId) {
        Map<Long, Account> accountsBySourceAccountId = new HashMap<>();
        int syncedCount = 0;

        for (RemoteAccount remoteAccount : remoteAccounts) {
            if (remoteAccount == null || remoteAccount.accountId() == null || remoteAccount.userId() == null) {
                continue;
            }

            Customer customer = customersBySourceUserId.get(remoteAccount.userId());
            if (customer == null) {
                continue;
            }

            Account account = accountRepository.findBySourceAccountId(remoteAccount.accountId())
                    .orElseGet(Account::new);

            account.setSourceAccountId(remoteAccount.accountId());
            account.setCustomerId(customer.getCustomerId());
            account.setAccountNumber(defaultValue(remoteAccount.accountNumber(), "SYNC-" + remoteAccount.accountId()));
            account.setAccountType(defaultValue(remoteAccount.accountType(), "SAVINGS"));
            account.setBalance(remoteAccount.balance() == null ? 0.0 : remoteAccount.balance());

            Account saved = accountRepository.save(account);
            accountsBySourceAccountId.put(remoteAccount.accountId(), saved);
            syncedCount++;
        }

        return new SyncAccountResult(accountsBySourceAccountId, syncedCount);
    }

    private SyncTransactionResult upsertTransactions(String adminToken, LocalDateTime since, Map<Long, Account> accountsBySourceAccountId) {
        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int pagesFetched = 0;

        int page = 0;
        boolean hasNext = true;
        while (hasNext) {
            RemotePagedTransactionResponse response = fetchTransactionsPage(adminToken, since, page);
            pagesFetched++;

            List<RemoteTransaction> remoteTransactions = response == null || response.transactions() == null
                    ? List.of()
                    : response.transactions();

            for (RemoteTransaction remoteTransaction : remoteTransactions) {
                if (remoteTransaction == null || remoteTransaction.id() == null || remoteTransaction.accountId() == null) {
                    skippedCount++;
                    continue;
                }

                Account sourceAccount = accountsBySourceAccountId.get(remoteTransaction.accountId());
                if (sourceAccount == null) {
                    skippedCount++;
                    continue;
                }

                Transaction transaction = transactionRepository.findBySourceTransactionId(remoteTransaction.id())
                        .orElseGet(Transaction::new);
                boolean exists = transaction.getId() != null;

                transaction.setSourceTransactionId(remoteTransaction.id());
                transaction.setAccountId(sourceAccount.getAccountId());
                transaction.setCounterpartyAccountId(resolveCounterpartyLocalId(remoteTransaction.counterpartyAccountId(), accountsBySourceAccountId));
                transaction.setAmount(remoteTransaction.amount() == null ? 0.0 : remoteTransaction.amount());
                transaction.setTransactionType(resolveTransactionType(remoteTransaction.transactionType()));
                transaction.setTransactionTime(remoteTransaction.transactionTime() == null ? LocalDateTime.now() : remoteTransaction.transactionTime());
                transaction.setChannel(defaultValue(remoteTransaction.channel(), "SYNC"));
                transactionRepository.save(transaction);

                if (exists) {
                    updatedCount++;
                } else {
                    insertedCount++;
                }
            }

            hasNext = response != null && response.hasNext();
            page++;

            if (remoteTransactions.isEmpty() && !hasNext) {
                break;
            }
            if (remoteTransactions.isEmpty() && hasNext) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "MockBank transactions export returned empty page with hasNext=true");
            }
        }

        return new SyncTransactionResult(insertedCount, updatedCount, skippedCount, pagesFetched);
    }

    private RemotePagedTransactionResponse fetchTransactionsPage(String adminToken, LocalDateTime since, int page) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/admin/export/transactions")
                                .queryParam("page", page)
                                .queryParam("size", syncPageSize);
                        if (since != null) {
                            uriBuilder.queryParam("since", since.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        return uriBuilder.build();
                    })
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(RemotePagedTransactionResponse.class);
        } catch (RestClientResponseException exception) {
            throw mapRemoteException(exception, "MockBank transactions export failed");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MockBank service is unavailable", exception);
        }
    }

    private Long resolveCounterpartyLocalId(Long sourceCounterpartyAccountId, Map<Long, Account> accountsBySourceAccountId) {
        if (sourceCounterpartyAccountId == null) {
            return null;
        }
        Account counterparty = accountsBySourceAccountId.get(sourceCounterpartyAccountId);
        return counterparty == null ? null : counterparty.getAccountId();
    }

    private Transaction.TransactionType resolveTransactionType(String remoteType) {
        if (isBlank(remoteType)) {
            return Transaction.TransactionType.OTHER;
        }

        String normalized = remoteType.trim().toUpperCase(Locale.ROOT);
        if ("WITHDRAW".equals(normalized)) {
            normalized = "WITHDRAWAL";
        }

        try {
            return Transaction.TransactionType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return Transaction.TransactionType.OTHER;
        }
    }

    private ResponseStatusException mapRemoteException(RestClientResponseException exception, String fallbackMessage) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String responseBody = exception.getResponseBodyAsString();
        String message = isBlank(responseBody) ? fallbackMessage : responseBody;
        return new ResponseStatusException(status, message, exception);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String defaultValue(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RemoteAdminAuthResponse(
            String token,
            String message,
            String email,
            String name
    ) {
    }

    private record RemoteUser(
            Long id,
            String name,
            String email,
            String role
    ) {
    }

    private record RemoteAccount(
            Long accountId,
            Long userId,
            String accountNumber,
            String accountType,
            Double balance
    ) {
    }

    private record RemoteTransaction(
            Long id,
            Long accountId,
            Long counterpartyAccountId,
            Double amount,
            String transactionType,
            LocalDateTime transactionTime,
            String channel
    ) {
    }

    private record RemotePagedTransactionResponse(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            List<RemoteTransaction> transactions
    ) {
    }

    private record SyncCustomerResult(
            Map<Long, Customer> customersBySourceUserId,
            int syncedCount
    ) {
    }

    private record SyncAccountResult(
            Map<Long, Account> accountsBySourceAccountId,
            int syncedCount
    ) {
    }

    private record SyncTransactionResult(
            int insertedCount,
            int updatedCount,
            int skippedCount,
            int pagesFetched
    ) {
    }
}
