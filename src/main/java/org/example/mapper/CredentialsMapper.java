package org.example.mapper;

import org.example.dto.CredentialsRequestDTO;
import org.example.model.Credentials;
import org.example.util.Constant;
import org.example.util.PasswordService;

import java.util.UUID;

public class CredentialsMapper {
    public static Credentials generateCredentials(CredentialsRequestDTO credDto, UUID personId) {
        byte[] salt = PasswordService.generateSalt();
        byte[] passwordHash = PasswordService.hashPassword(credDto.password(), salt, Constant.HASH_ITERATIONS);

        return new Credentials(
                personId,
                credDto.login(),
                passwordHash,
                salt,
                Constant.HASH_ITERATIONS
        );

    }
}
