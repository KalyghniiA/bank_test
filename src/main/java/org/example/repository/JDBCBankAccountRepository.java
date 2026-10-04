package org.example.repository;

import org.example.exceptions.RepositoryParamException;
import org.example.exceptions.RepositoryException;
import org.example.exceptions.SQLTransactionException;
import org.example.model.BankAccount;
import org.example.model.CheckingAccount;
import org.example.model.SavingAccount;
import org.example.util.AccountType;
import org.example.util.Dictionaries;
import org.example.util.AccountStatus;
import org.example.util.transaction_manager.ConnectionHolder;


import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class JDBCBankAccountRepository implements Repository<UUID, BankAccount> {

    @Override
    public void save(UUID id, BankAccount item) {
        String sql = """
                insert into bank_account(id, type, person_id, balance, status)
                    values (?,
                            (select id from bank_account_type where name = ?),
                            ?,
                            ?,
                            (select id from bank_account_status where name = ?)
                            )
                """;
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, item.getAccountType().getMessage());
            ps.setObject(3, item.getUserId());
            ps.setBigDecimal(4, item.getBalance());
            ps.setString(5, item.getStatus().getMessage());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }

        if (item.getAccountType().equals(AccountType.SAVING) && item instanceof SavingAccount savingAccount) {
            String sqlSaving = """
                    insert into saving_account_details (account_id, withdraw_limit, max_withdraw_limit, date_last_accrual) values (?, ?, ?, ?);
                    """;
            try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sqlSaving)) {
                ps.setObject(1, id);
                ps.setObject(2, savingAccount.getWithdrawLimit());
                ps.setObject(3, savingAccount.getMaxWithdrawalLimit());
                ps.setObject(4, savingAccount.getDateLastAccrual());

                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RepositoryException("Произошла ошибка базы", e);
            }
        }

        if (item.getAccountType().equals(AccountType.CHECKING) && item instanceof CheckingAccount checkingAccount) {
            String sqlChecking = """
                        insert into checking_account_details (account_id, overdraft_limit) values (?, ?);
                        """;
            try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sqlChecking)) {
                ps.setObject(1, id);
                ps.setObject(2, checkingAccount.getOverdraftLimit());

                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RepositoryException("Произошла ошибка базы", e);
            }
        }

    }

    @Override
    public void delete(UUID id) {
        String sql = """
                update bank_account set status = (select id from bank_account_status where name = 'DELETE') where id = ?;
        """;

        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    @Override
    public Optional<BankAccount> get(UUID id) {
        String sql = """

                select * from bank_account
                left join saving_account_details on bank_account.id = saving_account_details.account_id
                left join checking_account_details on bank_account.id = checking_account_details.account_id
            where id = ?;
            """;
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {

            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();

            BankAccount item = null;
            if (rs.next()) {
                item = mapping(rs);
            }

            return Optional.ofNullable(item);
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }

    }

    @Override
    public  List<BankAccount> getAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(BankAccount oldItem, BankAccount newItem) {
        String sqlSelect =
                """
                select * from bank_account
                left join saving_account_details on bank_account.id = saving_account_details.account_id
                left join checking_account_details on bank_account.id = checking_account_details.account_id
                where id = ? for update of bank_account;
                """;
        Connection conn = ConnectionHolder.get();
            try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                ps.setObject(1, oldItem.getId());
                ResultSet rs = ps.executeQuery();
                BankAccount oldAccForDB = null;
                while (rs.next()) {
                    oldAccForDB = mapping(rs);
                }
                if (!oldItem.equals(oldAccForDB)) throw new SQLTransactionException("Данные уже были изменены, попробуйте снова");
            } catch (SQLException e) {
                throw new RepositoryException("Произошла ошибка базы", e);
            }

        String sql = """
                            update bank_account set type = ?,
                                                    person_id = ?,
                                                    balance = ?,
                                                    status = ?
                            where id = ?;
                            """;

            int typeId = Dictionaries.typeAccountDictionary.entrySet().stream()
                    .filter(elem -> elem.getValue().equals(newItem.getAccountType().getMessage()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new RepositoryParamException("Передан неизвестный тип аккаунта"));
            int statusId = Dictionaries
                    .statusAccountDictionary.entrySet().stream()
                    .filter(elem -> elem.getValue().equals(newItem.getStatus().getMessage()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new RepositoryParamException("Передан неизвестный статус аккаунта"));

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1,typeId);
                ps.setObject(2, newItem.getUserId());
                ps.setBigDecimal(3, newItem.getBalance());
                ps.setInt(4, statusId);
                ps.setObject(5, newItem.getId());

                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RepositoryException("Произошла ошибка базы", e);
            }
        switch (newItem.getAccountType()) {
                case CHECKING -> {
                    String sqlUpdate = "update checking_account_details set overdraft_limit =  ? where account_id = ?;";

                    try (PreparedStatement psCheck = conn.prepareStatement(sqlUpdate)) {
                        psCheck.setObject(1, ((CheckingAccount) newItem).getOverdraftLimit());
                        psCheck.setObject(2, newItem.getId());
                        psCheck.executeUpdate();
                    } catch (SQLException e) {
                        throw new RepositoryException("Произошла ошибка базы", e);
                    }
                }
                case SAVING -> {
                    String sqlUpdate = """
                            update saving_account_details set withdraw_limit = ?,
                                                              max_withdraw_limit = ?,
                                                              date_last_accrual = ?
                            where account_id = ?;
                            """;
                    try (PreparedStatement psSaving = conn.prepareStatement(sqlUpdate)) {
                        psSaving.setObject(1, ((SavingAccount) newItem).getWithdrawLimit());
                        psSaving.setObject(2, ((SavingAccount) newItem).getMaxWithdrawalLimit());
                        psSaving.setTimestamp(3, Timestamp.valueOf(((SavingAccount) newItem).getDateLastAccrual()));
                        psSaving.setObject(4, newItem.getId());
                        psSaving.executeUpdate();
                    } catch (SQLException e) {
                        throw new RepositoryException("Произошла ошибка базы", e);
                    }
                }
            }
    }


    public List<BankAccount> getByAccountType(AccountType accountType) {
        List<BankAccount> result = new ArrayList<>();
        String sql = """
                select * from bank_account
                    left join saving_account_details on bank_account.id = saving_account_details.account_id
                    left join checking_account_details on bank_account.id = checking_account_details.account_id
                where type = ?;
                """;

        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            Integer typeId = Dictionaries.typeAccountDictionary.entrySet()
                    .stream()
                    .filter(elem -> elem.getValue().equals(accountType.getMessage()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new RepositoryParamException("Произошла ошибка типа: Проверьте верность передаваемого типа"));

            ps.setObject(1, typeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(mapping(rs));
            }

            return result;

        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    public List<BankAccount> getByUserId(UUID userId) {
        List<BankAccount> result = new ArrayList<>();
        String sql = """
                select * from bank_account
                    left join saving_account_details on bank_account.id = saving_account_details.account_id
                    left join
                checking_account_details on bank_account.id = checking_account_details.account_id
                where person_id
                = ?;
                """;
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(mapping(rs));
            }
            return  result;
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    public List<BankAccount> getByStatus(AccountStatus status) {
        List<BankAccount> result = new ArrayList<>();
        String sql = """
                select * from bank_account
                            left join saving_account_details on bank_account.id = saving_account_details.account_id
                           left join checking_account_details on bank_account.id = checking_account_details.account_id
                        where status = ?;
        """;
        Integer statusIndex = Dictionaries.statusAccountDictionary.entrySet().stream()
                .filter(elem -> elem.getValue().equals(status.getMessage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RepositoryParamException("Передан неизвестный статус, проверьте точность"));
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, statusIndex);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(mapping(rs));
            }
            return  result;
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }


    private BankAccount mapping(ResultSet rs) throws SQLException {
        UUID personId = UUID.fromString(rs.getString("person_id"));
        UUID accountId = UUID.fromString(rs.getString("id"));
        BigDecimal balance = rs.getBigDecimal("balance");
        AccountStatus status = AccountStatus.fromString(rs.getString("status"));
        switch (AccountType.fromString(Dictionaries.typeAccountDictionary.get(rs.getInt("type")))) {
            case AccountType.SAVING -> {
                int withdrawLimit = rs.getInt("withdraw_limit");
                int maxWithdrawLimit = rs.getInt("max_withdraw_limit");
                LocalDateTime dateLastAccrual = rs.getTimestamp("date_last_accrual").toLocalDateTime();
                return new SavingAccount(accountId, personId, balance, withdrawLimit, maxWithdrawLimit, dateLastAccrual, status);
            }
            case AccountType.CHECKING -> {
                BigDecimal overdraftLimit = rs.getBigDecimal("overdraft_limit");
                return new CheckingAccount(accountId, personId, balance, overdraftLimit, status);
            }
            default -> {
                return new BankAccount(accountId, personId, balance, AccountType.DEFAULT, status);
            }
        }
    }
    }

