package org.example.model;

import org.example.util.PersonStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Person {
    private final UUID id;
    private String firstName;
    private String subName;
    private String middleName;
    private LocalDate birthDate;
    private String phoneNumber;
    private String email;
    private PersonStatus status;

    public Person(String firstName,
                  String subName,
                  String middleName,
                  LocalDate birthDate,
                  String phoneNumber,
                  String email) {
        this.id = UUID.randomUUID();
        this.firstName = firstName;
        this.subName = subName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.status = PersonStatus.ACTIVE;
    }

    public Person(UUID id,
                  String firstName,
                  String subName,
                  String middleName,
                  LocalDate birthDate,
                  String phoneNumber,
                  String email) {
        this.id = id;
        this.firstName = firstName;
        this.subName = subName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.status = PersonStatus.ACTIVE;
    }

    public Person(UUID id,
                  String firstName,
                  String subName,
                  String middleName,
                  LocalDate birthDate,
                  String phoneNumber,
                  String email,
                  PersonStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.subName = subName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.status = status;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return subName;
    }

    public void setLastName(String subName) {
        this.subName = subName;
    }

    public UUID getId() {
        return id;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PersonStatus getStatus() {
        return status;
    }

    public void setStatus(PersonStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id) &&
               Objects.equals(firstName, person.firstName) &&
                Objects.equals(subName, person.subName) &&
                Objects.equals(middleName, person.middleName) &&
                Objects.equals(birthDate, person.birthDate) &&
                Objects.equals(phoneNumber, person.phoneNumber) &&
                Objects.equals(email, person.email) &&
                Objects.equals(status, person.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, subName, middleName, birthDate, phoneNumber, email, status);
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", subName='" + subName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", birthDate=" + birthDate +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
