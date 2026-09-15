// =============================================================================
// BankAccountTest.java — JUnit 5 Unit Tests
// Author: Aston Tellis | github.com/AstonTellis
// =============================================================================
// WHAT IS BEING TESTED:
//   - Normal deposits and withdrawals
//   - Overdraft protection (InsufficientFundsException)
//   - Method chaining (fluent interface)
//   - Account-to-account transfers
//   - Frozen account enforcement
//   - Input validation (negative amounts, null values, etc.)
//   - Transaction history accuracy
//   - Edge cases (zero balance, max limits)
//
// HOW TO RUN:
//   Requires JUnit 5 (junit-platform-console-standalone.jar) on classpath.
//   Or run via any IDE (IntelliJ, Eclipse, VS Code with Java extension).
// =============================================================================

package com.astontellis.banking;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BankAccount Unit Tests")
class BankAccountTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────
    private BankAccount account;
    private BankAccount secondAccount;

    @BeforeEach
    void setUp() {
        // Fresh account before each test — no shared state
        account       = new BankAccount("Aston Tellis", "CURRENT", 1000.00);
        secondAccount = new BankAccount("Jane Smith",   "SAVINGS", 500.00);
    }

    // ── Constructor Tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Account created with correct opening balance")
        void testOpeningBalance() {
            assertEquals(1000.00, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Account created with zero balance using default constructor")
        void testZeroOpeningBalance() {
            BankAccount zeroAccount = new BankAccount("Test User", "SAVINGS");
            assertEquals(0.0, zeroAccount.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Account holder name stored correctly")
        void testAccountHolder() {
            assertEquals("Aston Tellis", account.getAccountHolder());
        }

        @Test
        @DisplayName("Account ID is generated and not null")
        void testAccountIdNotNull() {
            assertNotNull(account.getAccountId());
            assertTrue(account.getAccountId().startsWith("ACC-"));
        }

        @Test
        @DisplayName("Two accounts have different IDs")
        void testUniqueAccountIds() {
            assertNotEquals(account.getAccountId(), secondAccount.getAccountId());
        }

        @Test
        @DisplayName("Negative opening balance throws IllegalArgumentException")
        void testNegativeOpeningBalance() {
            assertThrows(IllegalArgumentException.class,
                () -> new BankAccount("Test", "CURRENT", -100.0));
        }

        @Test
        @DisplayName("Empty account holder name throws IllegalArgumentException")
        void testEmptyHolderName() {
            assertThrows(IllegalArgumentException.class,
                () -> new BankAccount("", "CURRENT"));
        }

        @Test
        @DisplayName("Account is not frozen on creation")
        void testNotFrozenOnCreation() {
            assertFalse(account.isFrozen());
        }

        @Test
        @DisplayName("Opening deposit appears in transaction history")
        void testOpeningDepositInHistory() {
            assertEquals(1, account.getTransactionCount());
        }
    }

    // ── Deposit Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Valid deposit increases balance correctly")
        void testValidDeposit() {
            account.deposit(500.00);
            assertEquals(1500.00, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Multiple deposits accumulate correctly")
        void testMultipleDeposits() {
            account.deposit(100.00);
            account.deposit(200.00);
            account.deposit(300.00);
            assertEquals(1600.00, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Deposit returns same account (fluent interface)")
        void testDepositReturnsThis() {
            BankAccount result = account.deposit(100.00);
            assertSame(account, result);
        }

        @Test
        @DisplayName("Method chaining works correctly")
        void testMethodChaining() {
            account.deposit(100.00).deposit(200.00).deposit(300.00);
            assertEquals(1600.00, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Zero deposit throws IllegalArgumentException")
        void testZeroDeposit() {
            assertThrows(IllegalArgumentException.class,
                () -> account.deposit(0.0));
        }

        @Test
        @DisplayName("Negative deposit throws IllegalArgumentException")
        void testNegativeDeposit() {
            assertThrows(IllegalArgumentException.class,
                () -> account.deposit(-100.0));
        }

        @Test
        @DisplayName("Deposit exceeding limit throws IllegalArgumentException")
        void testDepositExceedsLimit() {
            assertThrows(IllegalArgumentException.class,
                () -> account.deposit(100_000.00));
        }

        @Test
        @DisplayName("Deposit on frozen account throws IllegalStateException")
        void testDepositFrozenAccount() {
            account.freeze();
            assertThrows(IllegalStateException.class,
                () -> account.deposit(100.00));
        }

        @Test
        @DisplayName("Deposit recorded in transaction history")
        void testDepositInHistory() {
            account.deposit(250.00);
            assertEquals(2, account.getTransactionCount()); // 1 opening + 1 deposit
        }
    }

    // ── Withdrawal Tests ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Withdrawal Tests")
    class WithdrawalTests {

        @Test
        @DisplayName("Valid withdrawal decreases balance correctly")
        void testValidWithdrawal() throws InsufficientFundsException {
            account.withdraw(300.00);
            assertEquals(700.00, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Withdrawing exact balance leaves zero")
        void testWithdrawExactBalance() throws InsufficientFundsException {
            account.withdraw(1000.00);
            assertEquals(0.0, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Overdraft throws InsufficientFundsException")
        void testOverdraftThrows() {
            assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(1500.00));
        }

        @Test
        @DisplayName("InsufficientFundsException contains correct amounts")
        void testExceptionAmounts() {
            InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> account.withdraw(2000.00)
            );
            assertEquals(2000.00, ex.getAmount(), 0.001);
            assertEquals(1000.00, ex.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Balance unchanged after failed withdrawal")
        void testBalanceUnchangedAfterFail() {
            assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(9999.00));
            assertEquals(1000.00, account.getBalance(), 0.001);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, -1.0, -100.0})
        @DisplayName("Non-positive withdrawal amounts throw IllegalArgumentException")
        void testNonPositiveWithdrawal(double amount) {
            assertThrows(IllegalArgumentException.class,
                () -> account.withdraw(amount));
        }

        @Test
        @DisplayName("Withdrawal on frozen account throws IllegalStateException")
        void testWithdrawalFrozenAccount() {
            account.freeze();
            assertThrows(IllegalStateException.class,
                () -> account.withdraw(100.00));
        }

        @Test
        @DisplayName("Withdrawal recorded in transaction history")
        void testWithdrawalInHistory() throws InsufficientFundsException {
            account.withdraw(100.00);
            assertEquals(2, account.getTransactionCount());
        }
    }

    // ── Transfer Tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Transfer Tests")
    class TransferTests {

        @Test
        @DisplayName("Transfer deducts from source correctly")
        void testTransferDeductsSource() throws InsufficientFundsException {
            account.transferTo(secondAccount, 300.00);
            assertEquals(700.00, account.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Transfer adds to target correctly")
        void testTransferAddsTarget() throws InsufficientFundsException {
            account.transferTo(secondAccount, 300.00);
            assertEquals(800.00, secondAccount.getBalance(), 0.001);
        }

        @Test
        @DisplayName("Transfer to self throws IllegalArgumentException")
        void testTransferToSelf() {
            assertThrows(IllegalArgumentException.class,
                () -> account.transferTo(account, 100.00));
        }

        @Test
        @DisplayName("Transfer to null throws IllegalArgumentException")
        void testTransferToNull() {
            assertThrows(IllegalArgumentException.class,
                () -> account.transferTo(null, 100.00));
        }

        @Test
        @DisplayName("Transfer with insufficient funds throws InsufficientFundsException")
        void testTransferInsufficientFunds() {
            assertThrows(InsufficientFundsException.class,
                () -> account.transferTo(secondAccount, 9999.00));
        }

        @Test
        @DisplayName("Both accounts record transfer in history")
        void testTransferRecordedBothSides() throws InsufficientFundsException {
            account.transferTo(secondAccount, 100.00);
            assertEquals(2, account.getTransactionCount());       // opening + transfer_out
            assertEquals(2, secondAccount.getTransactionCount()); // opening + transfer_in
        }
    }

    // ── Freeze / Unfreeze Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("Account Freeze Tests")
    class FreezeTests {

        @Test
        @DisplayName("Frozen account reports isFrozen() as true")
        void testFreezeSetsFrozen() {
            account.freeze();
            assertTrue(account.isFrozen());
        }

        @Test
        @DisplayName("Unfreezing restores normal operations")
        void testUnfreezeRestoresOps() throws InsufficientFundsException {
            account.freeze();
            account.unfreeze();
            assertFalse(account.isFrozen());
            account.deposit(100.00); // should not throw
            assertEquals(1100.00, account.getBalance(), 0.001);
        }
    }

    // ── Equality Tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Equality and Identity Tests")
    class EqualityTests {

        @Test
        @DisplayName("Same account equals itself")
        void testSameAccountEquals() {
            assertEquals(account, account);
        }

        @Test
        @DisplayName("Two different accounts are not equal")
        void testDifferentAccountsNotEqual() {
            assertNotEquals(account, secondAccount);
        }

        @Test
        @DisplayName("toString contains account ID and balance")
        void testToStringFormat() {
            String str = account.toString();
            assertTrue(str.contains(account.getAccountId()));
            assertTrue(str.contains("1000.00"));
        }
    }
}
