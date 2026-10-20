package org.example.mapper;

import org.example.dto.PersonRequestDTO;
import org.example.dto.PersonResponseDTO;
import org.example.model.Person;
import org.example.util.PersonStatus;

import java.util.UUID;

public class PersonMapper {
    public static Person toPerson(PersonRequestDTO dto) {
        return new Person(
            dto.firstName(),
                dto.subName(),
                dto.middleName(),
                dto.birthDate(),
                dto.number(),
                dto.email()
        );
    }

    public static Person toPerson(PersonRequestDTO dto, UUID personId) {
        return new Person(
                personId,
                dto.firstName(),
                dto.subName(),
                dto.middleName(),
                dto.birthDate(),
                dto.number(),
                dto.email()
        );
    }

    public static Person toPerson(PersonRequestDTO dto, UUID personId, PersonStatus status) {
        return new Person(
                personId,
                dto.firstName(),
                dto.subName(),
                dto.middleName(),
                dto.birthDate(),
                dto.number(),
                dto.email(),
                status
        );
    }

    public static PersonResponseDTO toPersonResponseDTO(Person person) {
        return new PersonResponseDTO(
                person.getFirstName(),
                person.getSubName(),
                person.getMiddleName(),
                person.getBirthDate(),
                person.getPhoneNumber(),
                person.getEmail()
        );
    }
}
