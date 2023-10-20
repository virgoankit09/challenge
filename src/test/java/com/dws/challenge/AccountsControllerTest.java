package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.math.MathContext;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  //tests for testing the transfer money api

  @Test
  void transferIntoEmptyAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":\"\",\"toAccountId\":\"Id-123\",\"amount\":1000}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferIntoNullAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":null,\"toAccountId\":\"Id-123\",\"amount\":1000}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferNegativeAmount() throws Exception {
    Account fromAccount = new Account("Id-123", new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account("Id-456", new BigDecimal("100"));
    this.accountsService.createAccount(toAccount);
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":\"Id-123\",\"toAccountId\":\"Id-456\",\"amount\":-20}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferZeroAmount() throws Exception {
    Account fromAccount = new Account("Id-123", new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account("Id-456", new BigDecimal("100"));
    this.accountsService.createAccount(toAccount);
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":\"Id-123\",\"toAccountId\":\"Id-456\",\"amount\":0}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferInvalidAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":\"Id-123\",\"toAccountId\":\"Id-123\",\"amount\":1000}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Account does not exist."));
  }

  @Test
  void transferAmountMoreThanBalance() throws Exception {
    Account fromAccount = new Account("Id-123", new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account("Id-456", new BigDecimal("100"));
    this.accountsService.createAccount(toAccount);
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":\"Id-123\",\"toAccountId\":\"Id-456\",\"amount\":2000}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Insufficient Balance"));
  }

  @Test
  void transferValidAmountIntoValidAccount(CapturedOutput output) throws Exception {
    Account fromAccount = new Account("Id-123", new BigDecimal("123"));
    this.accountsService.createAccount(fromAccount);
    Account toAccount = new Account("Id-456", new BigDecimal("100"));
    this.accountsService.createAccount(toAccount);
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromAccountId\":\"Id-123\",\"toAccountId\":\"Id-456\",\"amount\":20}"))
            .andExpect(status().isOk())
            .andExpect(content().string("Money transferred successfully."));

    assertThat(this.accountsService.getAccount("Id-123").getBalance()).isEqualTo( new BigDecimal(103));
    assertThat(this.accountsService.getAccount("Id-456").getBalance()).isEqualTo( new BigDecimal(120));

    assertThat(output).contains("Rs " +20+ " transferred to account "+toAccount.getAccountId());
    assertThat(output).contains("Rs " +20+ " transferred from account "+fromAccount.getAccountId());
  }

}
