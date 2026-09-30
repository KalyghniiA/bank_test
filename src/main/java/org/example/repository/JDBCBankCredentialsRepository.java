package org.example.repository;

import org.example.exceptions.RepositoryException;
import org.example.exceptions.SQLTransactionException;
import org.example.model.Credentials;
import org.example.util.transaction_manager.ConnectionHolder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JDBCBankCredentialsRepository implements CredentialsRepository<UUID, Credentials> {
    @Override
    public void save(UUID uuid, Credentials item) {
        String sql = "insert into credentials (id, person_id, login, password_hash, salt, iterations) values (?,?,?,?,?,?);";
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, uuid);
            ps.setObject(2, item.personId());
            ps.setString(3, item.login());
            ps.setObject(4, item.passwordHash());
            ps.setObject(5, item.salt());
            ps.setInt(6, item.iterations());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    @Override
    public void delete(UUID uuid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Optional<Credentials> get(UUID uuid) {
        String sql = "select * from credentials where id = ?;";
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, uuid);
            ResultSet rs = ps.executeQuery();
            Credentials cred = null;
            if (rs.next()) {
                cred = mapping(rs);
            }

            return Optional.ofNullable(cred);
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    @Override
    public Optional<Credentials> getByPersonId(UUID personId) {
        String sql = "select * from credentials where person_id = ?;";
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, personId);
            ResultSet rs = ps.executeQuery();
            Credentials cred = null;
            if (rs.next()) {
                cred = mapping(rs);
            }

            return Optional.ofNullable(cred);
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }


    @Override
    public Optional<Credentials> getByLogin(String login) {
        String sql = "select * from credentials where login = ?;";
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {
            ps.setObject(1, login);
            ResultSet rs = ps.executeQuery();
            Credentials cred = null;
            if (rs.next()) {
                cred = mapping(rs);
            }
            return Optional.ofNullable(cred);
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    @Override
    public void update(Credentials oldItem, Credentials newItem) {
        String sqlSelect = "select * from credentials where id = ?;";
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sqlSelect)) {
            ps.setObject(1, oldItem.id());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Credentials cred = mapping(rs);
                if (!oldItem.equals(cred)) throw new SQLTransactionException("ДАнные уже были изменены");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }

        String sql = """
                    update credentials set login = ?,
                                           password_hash = ?,
                                           salt = ?,
                                           iterations = ?
                    where id = ?;
                    """;
        try (PreparedStatement ps = ConnectionHolder.get().prepareStatement(sql)) {


            ps.setString(1, newItem.login());
            ps.setBytes(2, newItem.passwordHash());
            ps.setBytes(3, newItem.salt());
            ps.setInt(4, newItem.iterations());
            ps.setObject(5, newItem.id());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Произошла ошибка базы", e);
        }
    }

    @Override
    public List<Credentials> getAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private Credentials mapping (ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID personId = UUID.fromString(rs.getString("person_id"));
        String login = rs.getString("login");
        byte[] passwordHash = rs.getBytes("password_hash");
        byte[] salt = rs.getBytes("salt");
        int iterations = rs.getInt("iterations");

        return new Credentials(id, personId, login, passwordHash, salt, iterations);
    }
}
