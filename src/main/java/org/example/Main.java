package org.example;

import java.math.BigDecimal;


public class Main {

    private static final int ACCOUNTS_COUNT = 10;
    private static final int THREADS_COUNT = 20;
    private static final int TRANSFERS_PER_THREAD = 1000;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000");

    public static void main(String[] args) throws InterruptedException {

    }

    private static void test1() throws InterruptedException {
//        Repository<UUID, BankAccount> repository = new BankAccountRepository();
//        Repository<UUID, Transaction> transactionRepository = new TransactionRepository();
//        BankAccountService service = new BankAccountService(repository, transactionRepository);
//
//        List<BankAccount> accounts = new ArrayList<>();
//        for (int i = 0; i < ACCOUNTS_COUNT; i++) {
//            BankAccount account = new BankAccount(UUID.randomUUID(), INITIAL_BALANCE);
//            repository.save(account.getId(), account, ConnectionService.getConnection());
//            accounts.add(account);
//        }
//
//        BigDecimal initialTotal = sumBalances(accounts);
//        System.out.println("Начальная суммарная сумма по всем счетам: " + initialTotal);
//
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        Thread[] threads = new Thread[THREADS_COUNT];
//        for (int t = 0; t < THREADS_COUNT; t++) {
//            threads[t] = new Thread(() -> {
//                Random random = new Random();
//                for (int i = 0; i < TRANSFERS_PER_THREAD; i++) {
//                    int fromIndex = random.nextInt(ACCOUNTS_COUNT);
//                    int toIndex = random.nextInt(ACCOUNTS_COUNT);
//                    if (fromIndex == toIndex) {
//                        continue;
//                    }
//
//                    UUID fromId = accounts.get(fromIndex).getId();
//                    UUID toId = accounts.get(toIndex).getId();
//                    BigDecimal amount = new BigDecimal(1 + random.nextInt(100));
//
//                    try {
//                        service.transfer(fromId, toId, amount);
//                        successCount.incrementAndGet();
//                    } catch (RuntimeException e) {
//                        // Ожидаемые бизнес-исключения (недостаточно средств и т.д.) —
//                        // не баг, просто эта попытка перевода не удалась, поток продолжает работу.
//                        failCount.incrementAndGet();
//                    }
//                }
//            });
//        }
//
//        for (Thread thread : threads) {
//            thread.start();
//        }
//        for (Thread thread : threads) {
//            thread.join();
//        }
//
//        BigDecimal finalTotal = sumBalances(accounts);
//        System.out.println("Успешных переводов: " + successCount.get());
//        System.out.println("Неудачных попыток (ожидаемо, например BalanceLimitException): " + failCount.get());
//        System.out.println("Итоговая суммарная сумма по всем счетам: " + finalTotal);
//
//        if (initialTotal.compareTo(finalTotal) == 0) {
//            System.out.println("OK: суммарный баланс не изменился, деньги не потеряны и не размножены.");
//        } else {
//            System.out.println("FAIL: суммарный баланс изменился! Разница: " + finalTotal.subtract(initialTotal));
//        }
//    }
//
//    private static BigDecimal sumBalances(List<BankAccount> accounts) {
//        BigDecimal sum = BigDecimal.ZERO;
//        for (BankAccount account : accounts) {
//            sum = sum.add(account.getBalance());
//        }
//        return sum;
//    }

    }
}
