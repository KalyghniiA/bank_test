package org.example.repository;

import org.example.exceptions.BankAccountExistsException;
import org.example.model.BankAccount;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankAccountRepository {
    private final ConcurrentHashMap<UUID, BankAccount> account = new ConcurrentHashMap<>();

    public BankAccount findById(UUID id) {
        return account.get(id);
    }

    public void removeAccount(UUID id) {
        account.remove(id);
    }

    public void addAccount(BankAccount bankAccount) {
        if (account.containsKey(bankAccount.getId())) {
            throw new BankAccountExistsException("Account already exists");
        }

        account.put(bankAccount.getId(), bankAccount);
    }
}
