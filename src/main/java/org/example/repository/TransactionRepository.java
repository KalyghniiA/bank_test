package org.example.repository;

import org.example.exceptions.RepositoryItemExistsException;
import org.example.exceptions.RepositoryParamException;
import org.example.model.Transaction;
import org.example.util.ConnectionService;
import org.example.util.Dictionaries;
import org.example.util.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionRepository implements Repository<UUID, Transaction> {

    @Override
    public void save(UUID transactionId, Transaction item, Connection conn) throws SQLException {

        String sql = """
                insert into "transaction"\s
                    (id, account_id, type, amount, related_account_id)
                    values (?, ?, ?, ?, ?);
                """;

        int typeId = Dictionaries.typeTransactionDictionary.entrySet().stream()
                .filter(elem -> elem.getValue().equals(item.getType().getMessage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RepositoryParamException("Передан неизвестный тип транзакции"));

        try ( PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, item.getTransactionId());
            ps.setObject(2, item.getAccountId());
            ps.setInt(3, typeId);
            ps.setBigDecimal(4, item.getAmount());
            if (item.getRelatedAccountId() != null) {
                ps.setObject(5, item.getRelatedAccountId());
            } else {
                ps.setObject(5, null, Types.OTHER);
            }

            ps.executeUpdate();
        }
//        } catch (SQLException e) {
//            switch (e.getSQLState()) {
//                case "23502" -> throw new RepositoryParamException("Один из обязательных параметров пустой");
//                case "23505" -> throw new RepositoryParamException("Такой ключ уже есть в базе");
//                default -> throw new RuntimeException("Другая ошибка базы");
//            }
//        }

    }

    @Override
    public void delete(UUID uuid, Connection conn) throws SQLException {
        throw new UnsupportedOperationException("Транзакции нельзя удалить");
    }

    @Override
    public Optional<Transaction> get(UUID uuid, Connection conn) throws SQLException {
        String sql = """
                select * from "transaction" where id = ?;
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            ResultSet rs = ps.executeQuery();
            Transaction transaction = null;
            while (rs.next()) {
                transaction = mapping(rs);
            }

            return Optional.ofNullable(transaction);
        }
//        } catch (SQLException e) {
//            switch (e.getSQLState()) {
//                case "23503" -> throw new RepositoryParamException("Данного транзакции нет в базе");
//                default -> throw new RuntimeException("Другая ошибка базы", e);
//            }
//        }
    }

    @Override
    public void update(Transaction oldItem, Transaction newItem, Connection connection) {
        throw new UnsupportedOperationException("Транзакции нельзя обновлять");
    }

    @Override
    public List<Transaction> getAll(Connection connection) throws SQLException {
        throw new UnsupportedOperationException("Данная операции не поддерживается, используйте методы с сужением");
    }

    public List<Transaction> getByAccountId(UUID accountId, Connection conn) throws SQLException {
        List<Transaction> result = new ArrayList<>();
        String sql = "select * from \"transaction\" where account_id = ?;";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setObject(1, accountId);
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                Transaction transaction = mapping(rs);
                result.add(transaction);
            }

            return result;
        }
//        } catch (SQLException e) {
//            switch (e.getSQLState()) {
//                case "23503" -> throw new RepositoryParamException("У данного счета не было транзакций, либо данный аккаунт не существует");
//                default ->  throw new RuntimeException("Другая ошибка базы", e);
//            }
//        }
    }

    public List<Transaction> getByType(TransactionType type, Connection conn) throws SQLException {
        List<Transaction> result = new ArrayList<>();
        int transactionTypeId = Dictionaries.typeTransactionDictionary.entrySet().stream()
                .filter(elem -> elem.getValue().equals(type.getMessage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RepositoryParamException("Передан неизвестный тип транзакции"));

        String sql = "select * from \"transaction\" where type = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, transactionTypeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction transaction = mapping(rs);
                result.add(transaction);
            }

            return result;
        }
//        } catch (SQLException e) {
//            throw new RuntimeException("Другая ошибка базы", e);
//        }
    }

    public List<Transaction> getByAccountIdAndType(UUID accountId, TransactionType type, Connection conn) throws SQLException {
        List<Transaction> result = new ArrayList<>();
        int transactionTypeId = Dictionaries.typeTransactionDictionary.entrySet().stream()
                .filter(elem -> elem.getValue().equals(type.getMessage()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RepositoryParamException("Передан неизвестный тип транзакции"));

        String sql = "select * from \"transaction\" where account_id = ? and type = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, accountId);
            ps.setObject(2, transactionTypeId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction transaction = mapping(rs);
                result.add(transaction);
            }

            return result;
        }
//        } catch (SQLException e) {
//            switch (e.getSQLState()) {
//                case "23503" -> throw new RepositoryParamException("У данного счета не было транзакций, либо данный аккаунт не существует");
//                default ->  throw new RuntimeException("Другая ошибка базы", e);
//            }
//        }
    }


    private Transaction mapping (ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID accountId = UUID.fromString(rs.getString("account_id"));
        TransactionType type = TransactionType.fromString(Dictionaries.typeTransactionDictionary.get(rs.getInt("type")));
        BigDecimal amount = rs.getBigDecimal("amount");
        LocalDateTime timeStamp = rs.getTimestamp("timestep").toLocalDateTime();
        UUID relatedAccountId = rs.getString("related_account_id") == null ?  null : UUID.fromString(rs.getString("related_account_id"));

        return new Transaction(id, accountId, type, amount, timeStamp, relatedAccountId);
    }
}
