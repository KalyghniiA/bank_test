package org.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

public class Dictionaries {
    private final static Logger logger = LoggerFactory.getLogger(Dictionaries.class);
    public final static ConcurrentHashMap<Integer, String> statusAccountDictionary = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<Integer, String> typeAccountDictionary = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<Integer, String> typeTransactionDictionary = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<Integer, String> statusPersonDictionary = new ConcurrentHashMap<>();

    public static void loadDictionary() {
        try (Connection conn = ConnectionService.getConnection(); Statement statement = conn.createStatement()) {
            statusAccountDictionary.clear();
            typeAccountDictionary.clear();
            typeTransactionDictionary.clear();
            statusPersonDictionary.clear();

            ResultSet setTypeAccount = statement.executeQuery("select * from bank_account_type");

            while (setTypeAccount.next()) {
                String type = setTypeAccount.getString("name");
                int id = setTypeAccount.getInt("id");
                typeAccountDictionary.put(id, type);
            }

            ResultSet setStatusAccount = statement.executeQuery("select * from bank_account_status");

            while (setStatusAccount.next()) {
                String status = setStatusAccount.getString("name");
                int id = setStatusAccount.getInt("id");
                statusAccountDictionary.put(id, status);
            }

            ResultSet setTypeTransaction = statement.executeQuery("select * from transaction_type");

            while (setTypeTransaction.next()) {
                String status = setTypeTransaction.getString("name");
                int id = setTypeTransaction.getInt("id");
                typeTransactionDictionary.put(id, status);
            }

            ResultSet setStatusPerson = statement.executeQuery("select * from person_status");

            while (setStatusPerson.next()) {
                String status = setStatusPerson.getString("name");
                int id = setStatusPerson.getInt("id");
                statusPersonDictionary.put(id, status);
            }

        } catch (SQLException e) {
            switch (e.getSQLState()) {
                case "42703" -> logger.error("Один из параметров словарей не верен", e);
                case "42P01" -> logger.error("Один из словарей отсутствует в базе", e);
                default -> logger.error("Ошибка загрузки словарей", e);
            }

            throw new RuntimeException(e);
        }
    }
}
