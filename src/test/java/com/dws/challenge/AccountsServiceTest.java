package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private NotificationService notificationService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void transfer_failsOnInvalidAccountId() {
    try {
      this.accountsService.transfer("Id-123", "Id-456", new BigDecimal(20));
      fail("Should have failed when adding transferring to invalid account");
    } catch (AccountNotFoundException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account does not exist.");
    }
  }

  @Test
  void transfer_failsOnInsufficientBalance() {
    try {

      Account account = new Account("Id-1234");
      account.setBalance(new BigDecimal(1000));
      this.accountsService.createAccount(account);

      Account account2 = new Account("Id-4567");
      account2.setBalance(new BigDecimal(1000));
      this.accountsService.createAccount(account2);

      this.accountsService.transfer("Id-1234", "Id-4567", new BigDecimal(2000));
      fail("Should have failed when transferring more amount");
    } catch (InsufficientBalanceException ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient Balance");
    }
  }

  @Test
  void transfer(CapturedOutput output) {
      Account account = new Account("Id-12345");
      account.setBalance(new BigDecimal(1000));
      this.accountsService.createAccount(account);

      Account account2 = new Account("Id-45678");
      account2.setBalance(new BigDecimal(1000));
      this.accountsService.createAccount(account2);

      this.accountsService.transfer("Id-12345", "Id-45678", new BigDecimal(500));

      assertThat(this.accountsService.getAccount("Id-12345").getBalance()).isEqualTo(new BigDecimal(500));
      assertThat(this.accountsService.getAccount("Id-45678").getBalance()).isEqualTo(new BigDecimal(1500));

      assertThat(output).contains("Rs " +500+ " transferred to account "+account2.getAccountId());
      assertThat(output).contains("Rs " +500+ " transferred from account "+account.getAccountId());
  }

}
