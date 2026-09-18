-- Моковые данные для ручного тестирования (getByPersonId/getByType, update, soft delete).
-- Специально НЕ подключен в docker-compose.yml как /docker-entrypoint-initdb.d — тот
-- каталог выполняется только один раз при инициализации пустого volume, а у тебя в
-- контейнере уже есть рабочие данные. Применяй вручную, когда нужно:
--
--   docker cp src/main/resources/mock-data.sql database_bank:/mock-data.sql
--   docker exec -it database_bank psql -U user -d bank -f /mock-data.sql
--
-- (замени user/bank, если отличаются от .env). Скрипт идемпотентен по id — при повторном
-- запуске упадёт на уникальности id, это ожидаемо (не задумывался как upsert).

-- 4 персоны
insert into person (id, first_name, surname, middle_name, birth_date, phone_number, email) values
  ('11111111-1111-1111-1111-111111111111', 'Иван', 'Иванов', 'Иванович', '1990-03-12', '+79990000001', 'ivanov@example.com'),
  ('22222222-2222-2222-2222-222222222222', 'Мария', 'Петрова', 'Сергеевна', '1988-07-24', '+79990000002', 'petrova@example.com'),
  ('33333333-3333-3333-3333-333333333333', 'Пётр', 'Сидоров', 'Николаевич', '1995-11-02', '+79990000003', 'sidorov@example.com'),
  ('44444444-4444-4444-4444-444444444444', 'Анна', 'Кузнецова', 'Викторовна', '1992-01-30', '+79990000004', 'kuznecova@example.com');

-- 7 счетов: разные типы/статусы/владельцы (у p1 и p3 — по два счёта, чтобы было что
-- фильтровать через getByPersonId; a6 сразу в статусе DELETE — для проверки soft delete)
insert into bank_account (id, type, person_id, balance, status) values
  ('a1111111-0000-0000-0000-000000000001', (select id from bank_account_type where name = 'DEFAULT'),  '11111111-1111-1111-1111-111111111111', 1500.00,  (select id from bank_account_status where name = 'ACTIVE')),
  ('a1111111-0000-0000-0000-000000000002', (select id from bank_account_type where name = 'SAVING'),   '11111111-1111-1111-1111-111111111111', 50000.00, (select id from bank_account_status where name = 'ACTIVE')),
  ('a1111111-0000-0000-0000-000000000003', (select id from bank_account_type where name = 'CHECKING'), '22222222-2222-2222-2222-222222222222', 2000.00,  (select id from bank_account_status where name = 'ACTIVE')),
  ('a1111111-0000-0000-0000-000000000004', (select id from bank_account_type where name = 'SAVING'),   '33333333-3333-3333-3333-333333333333', 120000.00,(select id from bank_account_status where name = 'ACTIVE')),
  ('a1111111-0000-0000-0000-000000000005', (select id from bank_account_type where name = 'CHECKING'), '33333333-3333-3333-3333-333333333333', 300.00,   (select id from bank_account_status where name = 'BLOCKED')),
  ('a1111111-0000-0000-0000-000000000006', (select id from bank_account_type where name = 'DEFAULT'),  '44444444-4444-4444-4444-444444444444', 0.00,     (select id from bank_account_status where name = 'DELETE')),
  ('a1111111-0000-0000-0000-000000000007', (select id from bank_account_type where name = 'SAVING'),   '44444444-4444-4444-4444-444444444444', 8000.00,  (select id from bank_account_status where name = 'ACTIVE'));

-- Детали SAVING-счетов: у a1111111-...-004 date_last_accrual специально старше 30 дней
-- (годится сразу проверить будущую выборку "кому пора начислить проценты", BANK-16)
insert into saving_account_details (account_id, withdraw_limit, max_withdraw_limit, date_last_accrual) values
  ('a1111111-0000-0000-0000-000000000002', 3, 3, now() - interval '10 days'),
  ('a1111111-0000-0000-0000-000000000004', 1, 6, now() - interval '45 days'),
  ('a1111111-0000-0000-0000-000000000007', 6, 6, now() - interval '5 days');

-- Детали CHECKING-счетов
insert into checking_account_details (account_id, overdraft_limit) values
  ('a1111111-0000-0000-0000-000000000003', 10000.00),
  ('a1111111-0000-0000-0000-000000000005', 5000.00);

-- Немного истории операций (только DEPOSIT/WITHDRAW — не стал добавлять TRANSFER_IN/OUT,
-- у их текущей маркировки в BankAccountService направление выглядит спорно, не хотел
-- закреплять это в тестовых данных молча)
insert into "transaction" (id, account_id, type, amount, timestep, related_account_id) values
  ('b1111111-0000-0000-0000-000000000001', 'a1111111-0000-0000-0000-000000000001', (select id from transaction_type where name = 'DEPOSIT'),  1500.00, now() - interval '5 days', null),
  ('b1111111-0000-0000-0000-000000000002', 'a1111111-0000-0000-0000-000000000003', (select id from transaction_type where name = 'WITHDRAW'), 500.00,  now() - interval '3 days', null),
  ('b1111111-0000-0000-0000-000000000003', 'a1111111-0000-0000-0000-000000000004', (select id from transaction_type where name = 'DEPOSIT'),  20000.00,now() - interval '20 days', null);
