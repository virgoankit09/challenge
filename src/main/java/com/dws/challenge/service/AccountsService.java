package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /*
    service method for money transfer
   */
  public void transfer(String fromAccountId, String toAccountId, BigDecimal transferAmount) {
    String lock1, lock2;
    //ensuring lock is taken over existing string in the pool
    fromAccountId = fromAccountId.intern();
    toAccountId = toAccountId.intern();

    //retrieving accounts from the repo (map)
    Account fromAccount = accountsRepository.getAccount(fromAccountId);
    Account toAccount = accountsRepository.getAccount(toAccountId);

    //throwing exception if account id mentioned in request does not exist in the map
    if(Objects.isNull(fromAccount) || Objects.isNull(toAccount)) {
      throw new AccountNotFoundException("Account does not exist.");
    }

    if (Long.valueOf(fromAccountId.split("-")[1]) < Long.valueOf(toAccountId.split("-")[1])) {
      lock1 = fromAccountId;
      lock2 = toAccountId;
    } else {
      lock1 = toAccountId;
      lock2 = fromAccountId;
    }

    //acquiring locks in order
    synchronized(lock1) {
      synchronized(lock2) {
        //checking balance of the sender's account and throwing exception for insufficient balance
        BigDecimal fromAccountBalance = fromAccount.getBalance();
        if (fromAccountBalance.compareTo(transferAmount) == -1) {
          throw new InsufficientBalanceException("Insufficient Balance");
        }

        //debit and credit amount from the balance
        BigDecimal toValue = toAccount.getBalance();
        fromAccount.setBalance(fromAccountBalance.add(transferAmount.negate()));
        toAccount.setBalance(toValue.add(transferAmount));

        //notifying both sender and receiver about the transaction
        notificationService.notifyAboutTransfer(fromAccount, "Rs " +transferAmount+ " transferred to account "+toAccount.getAccountId());
        notificationService.notifyAboutTransfer(toAccount,  "Rs " +transferAmount+ " transferred from account "+fromAccount.getAccountId());
      }
    }
  }

}
