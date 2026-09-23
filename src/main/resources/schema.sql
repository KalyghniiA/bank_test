CREATE TABLE bank_account (
  id uuid PRIMARY KEY NOT NULL,
  type bigint not null,
  person_id uuid not null ,
  balance numeric(15, 2) not null default 0,
  status bigint not null,
  opening_date timestamp not null default now()
);

CREATE TABLE bank_account_type (
  id bigserial PRIMARY KEY,
  name varchar(100) not null
);

CREATE TABLE bank_account_status (
  id bigserial PRIMARY KEY,
  name varchar(100) not null
);

CREATE TABLE person (
  id uuid PRIMARY KEY,
  first_name varchar(100) not null ,
  surname varchar(100) not null ,
  middle_name varchar(100),
  birth_date date not null,
  phone_number varchar(12),
  email varchar(100),
  status bigint not null
);

CREATE TABLE "transaction" (
  id uuid PRIMARY KEY,
  account_id uuid not null ,
  type bigint not null ,
  amount numeric(15, 2) not null,
  timestep timestamp not null default now(),
  related_account_id uuid
);

CREATE TABLE transaction_type (
  id bigserial PRIMARY KEY,
  name varchar(100) not null
);

create table person_status (
   id bigserial primary key,
   name varchar(100) not null
);

create table saving_account_details (
    account_id uuid primary key,
    withdraw_limit integer not null check(withdraw_limit >= 0),
    max_withdraw_limit integer not null check ( withdraw_limit <= max_withdraw_limit and max_withdraw_limit >= 0),
    date_last_accrual timestamp,
    foreign key(account_id) references bank_account(id)
);

create table checking_account_details (
    account_id uuid primary key,
    overdraft_limit numeric(15,2),
    foreign key (account_id) references bank_account(id)
);

create table credentials (
    id uuid primary key,
    person_id uuid references person(id) unique not null,
    login varchar(100) unique not null,
    password_hash bytea not null,
    salt bytea not null,
    iterations int not null default 600000
);


ALTER TABLE bank_account ADD FOREIGN KEY (type) REFERENCES bank_account_type (id) DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE bank_account ADD FOREIGN KEY (status) REFERENCES bank_account_status (id) DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE bank_account ADD FOREIGN KEY (person_id) REFERENCES person (id) DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "transaction" ADD FOREIGN KEY (account_id) REFERENCES bank_account (id) DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "transaction" ADD FOREIGN KEY (related_account_id) REFERENCES bank_account (id) DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "transaction" ADD FOREIGN KEY (type) REFERENCES transaction_type (id) DEFERRABLE INITIALLY IMMEDIATE;

alter table person add foreign key (status) references person_status (id) deferrable initially immediate;

insert into transaction_type (name) values ('DEPOSIT'), ('WITHDRAW'), ('TRANSFER_IN'), ('TRANSFER_OUT');
insert into bank_account_type (name) values ('DEFAULT'), ('SAVING'), ('CHECKING');
insert into bank_account_status (name) values ('ACTIVE'), ('BLOCKED'), ('DELETE');
insert into person_status (name) values ('ACTIVE'), ('BLOCKED');