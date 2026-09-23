package org.example.mapper;

import org.example.dto.AccountRequestDTO;
import org.example.exceptions.MappingDTOException;
import org.example.model.BankAccount;
import org.example.model.CheckingAccount;
import org.example.model.SavingAccount;
import org.example.util.AccountField;
import org.example.util.AccountStatus;
import org.example.util.AccountType;

import java.math.BigDecimal;

public class  AccountMapper {
    public static BankAccount toBankAccount(AccountRequestDTO dto) {
        switch (AccountType.fromString(dto.type())) {
            case SAVING -> {
                return new SavingAccount(
                        dto.personId(),
                        dto.balance(),
                        dto.maxWithdrawalLimit()
                );
            }
            case CHECKING -> {
                return new CheckingAccount(
                        dto.personId(),
                        dto.balance(),
                        dto.overdraftLimit()
                );
            }
            case DEFAULT -> {
                return new BankAccount(
                        dto.personId(),
                        dto.balance()
                );
            }
            default -> {
                throw new MappingDTOException("Данный тип аккаунта не поддерживается");
            }
        }
    }

    public static <T> BankAccount toNewBankAccount (BankAccount oldAcc, AccountField<T> param, T newValue) {
        return switch (oldAcc.getAccountType()) {
            case SAVING -> {
               yield new SavingAccount(
                       oldAcc.getId(),
                       oldAcc.getUserId(),
                       param.equals(AccountField.BALANCE) ? (BigDecimal) newValue : oldAcc.getBalance(),
                       ((SavingAccount)oldAcc).getWithdrawLimit(),
                       param.equals(AccountField.MAX_WITHDRAWAL_LIMIT) ? (Integer) newValue : ((SavingAccount) oldAcc).getMaxWithdrawalLimit(),
                       ((SavingAccount) oldAcc).getDateLastAccrual(),
                       param.equals(AccountField.STATUS) ?(AccountStatus)  newValue : oldAcc.getStatus()
               );
            }
            case CHECKING -> {
                yield new CheckingAccount(
                        oldAcc.getId(),
                        oldAcc.getUserId(),
                        param.equals(AccountField.BALANCE) ? (BigDecimal) newValue : oldAcc.getBalance(),
                        param.equals(AccountField.OVERDRAFT_LIMIT) ? (BigDecimal) newValue : ((CheckingAccount) oldAcc).getOverdraftLimit(),
                        param.equals(AccountField.STATUS) ? (AccountStatus) newValue : oldAcc.getStatus()

                );
            }
            default -> {
                yield new BankAccount(
                        oldAcc.getId(),
                        oldAcc.getUserId(),
                        param.equals(AccountField.BALANCE) ? (BigDecimal) newValue : oldAcc.getBalance(),
                        AccountType.DEFAULT,
                        param.equals(AccountField.STATUS) ? (AccountStatus) newValue : oldAcc.getStatus()
                );
            }
        };
    }
}
