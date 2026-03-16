package com.hcl.systembackend;

import com.jayway.jsonpath.JsonPath;
import com.hcl.systembackend.entity.Account;
import com.hcl.systembackend.entity.Customer;
import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.entity.Transaction.TransactionType;
import com.hcl.systembackend.repository.AccountRepository;
import com.hcl.systembackend.repository.CustomerRepository;
import com.hcl.systembackend.repository.FraudTransactionRepository;
import com.hcl.systembackend.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SystemBackendApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudTransactionRepository fraudTransactionRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void adminCanAuthenticateFetchHistoryAndPersistFlaggedAnomaly() throws Exception {
        fraudTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = new Customer();
        customer.setName("Alice");
        customer.setEmail("alice@example.com");
        customer.setPhone("9999999999");
        customer.setAddress("Chennai");
        customer.setCreatedAt(Timestamp.valueOf(LocalDateTime.now().minusDays(10)));
        customer = customerRepository.save(customer);

        Account account = new Account();
        account.setCustomerId(customer.getCustomerId());
        account.setAccountNumber("100200300");
        account.setAccountType("SAVINGS");
        account.setBalance(5000.0);
        account = accountRepository.save(account);

        Transaction normalTxn = new Transaction();
        normalTxn.setAccountId(account.getAccountId());
        normalTxn.setAmount(100.0);
        normalTxn.setTransactionType(TransactionType.DEPOSIT);
        normalTxn.setTransactionTime(LocalDateTime.now().minusHours(6));
        transactionRepository.save(normalTxn);

        Transaction anomalousTxn = new Transaction();
        anomalousTxn.setAccountId(account.getAccountId());
        anomalousTxn.setAmount(1000.0);
        anomalousTxn.setTransactionType(TransactionType.WITHDRAWAL);
        anomalousTxn.setTransactionTime(LocalDateTime.now().minusHours(1));
        transactionRepository.save(anomalousTxn);

        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Admin One",
                                  "email": "admin@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk());

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginBody, "$.token");

        mockMvc.perform(get("/api/admin/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flaggedFraud").value(true));

        assertThat(fraudTransactionRepository.findByTransactionId(anomalousTxn.getId())).isPresent();
    }
}
