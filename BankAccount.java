// =============================================================================
// BankAccount.java — Object-Oriented Java Portfolio Project
// Author: Aston Tellis | github.com/AstonTellis
// =============================================================================
// DESCRIPTION:
//   A clean OOP implementation of a bank account system demonstrating:
//   - Encapsulation (private fields, public getters/setters)
//   - Constructor overloading
//   - Exception handling (custom exceptions)
//   - Method chaining (fluent interface)
//   - toString / equals overrides
//   - JUnit 5 unit tests (see BankAccountTest.java)
//
// HOW TO COMPILE AND RUN:
//   javac BankAccount.java
//   java BankAccount
//
// TO RUN TESTS (requires JUnit 5 on classpath):
//   javac -cp junit-platform-console-standalone.jar BankAccountTest.java
//   java -jar junit-platform-console-standalone.jar --select-class=BankAccountTest
// =============================================================================

package com.astontellis.banking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ── Custom Exception ──────────────────────────────────────────────────────────
class InsufficientFundsException extends Exception {
    private final double amount;
    private final double balance;

    public InsufficientFundsException(double amount, double balance) {
        super(String.format(
            "Cannot withdraw €%.2f — available balance is €%.2f",
            amount, balance
        ));
        this.amount  = amount;
        this.balance = balance;
    }

    public double getAmount()  { return amount; }
    public double getBalance() { return balance; }
}

// ── Transaction Record ────────────────────────────────────────────────────────
class Transaction {
    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }

    private final Type          type;
    private final double        amount;
    private final double        balanceAfter;
    private final LocalDateTime timestamp;
    private final String        description;

    public Transaction(Type type, double amount, double balanceAfter, String description) {
        this.type         = type;
        this.amount       = amount;
        this.balanceAfter = balanceAfter;
        this.description  = description;
        this.timestamp    = LocalDateTime.now();
    }

    public Type          getType()         { return type; }
    public double        getAmount()       { return amount; }
    public double        getBalanceAfter() { return balanceAfter; }
    public String        getDescription()  { return description; }
    public LocalDateTime getTimestamp()    { return timestamp; }

    @Override
    public String toString() {
        String formatted = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sign      = (type == Type.DEPOSIT || type == Type.TRANSFER_IN) ? "+" : "-";
        return String.format("[%s] %-15s %s€%.2f  |  Balance: €%.2f  |  %s",
            formatted, type, sign, amount, balanceAfter, description);
    }
}

// ── BankAccount Class ─────────────────────────────────────────────────────────
public class BankAccount {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double MIN_BALANCE        = 0.0;
    private static final double MAX_WITHDRAWAL     = 10_000.0;
    private static final double MAX_DEPOSIT        = 50_000.0;

    // ── Fields (encapsulated) ─────────────────────────────────────────────────
    private final String        accountId;
    private final String        accountHolder;
    private final String        accountType;    // "CURRENT", "SAVINGS", "ISA"
    private       double        balance;
    private       boolean       frozen;
    private final List<Transaction> transactionHistory;

    // ── Static counter for unique IDs ─────────────────────────────────────────
    private static int idCounter = 1000;

    // ── Constructors (overloaded) ─────────────────────────────────────────────

    /** Standard account with zero initial balance */
    public BankAccount(String accountHolder, String accountType) {
        this(accountHolder, accountType, 0.0);
    }

    /** Account with opening balance */
    public BankAccount(String accountHolder, String accountType, double openingBalance) {
        if (accountHolder == null || accountHolder.isBlank()) {
            throw new IllegalArgumentException("Account holder name cannot be empty.");
        }
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }

        this.accountId          = "ACC-" + (++idCounter);
        this.accountHolder      = accountHolder.trim();
        this.accountType        = accountType.toUpperCase();
        this.balance            = openingBalance;
        this.frozen             = false;
        this.transactionHistory = new ArrayList<>();

        if (openingBalance > 0) {
            transactionHistory.add(new Transaction(
                Transaction.Type.DEPOSIT, openingBalance, openingBalance, "Opening deposit"
            ));
        }
    }

    // ── Core Operations ───────────────────────────────────────────────────────

    /**
     * Deposit money into the account.
     * Returns 'this' for method chaining: account.deposit(100).deposit(200)
     */
    public BankAccount deposit(double amount) {
        validateNotFrozen();
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        if (amount > MAX_DEPOSIT) {
            throw new IllegalArgumentException(
                String.format("Single deposit cannot exceed €%.2f.", MAX_DEPOSIT)
            );
        }

        balance += amount;
        transactionHistory.add(new Transaction(
            Transaction.Type.DEPOSIT, amount, balance, "Deposit"
        ));
        return this; // fluent interface
    }

    /**
     * Withdraw money from the account.
     * Throws InsufficientFundsException if balance would go below minimum.
     */
    public BankAccount withdraw(double amount) throws InsufficientFundsException {
        validateNotFrozen();
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if (amount > MAX_WITHDRAWAL) {
            throw new IllegalArgumentException(
                String.format("Single withdrawal cannot exceed €%.2f.", MAX_WITHDRAWAL)
            );
        }
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(amount, balance);
        }

        balance -= amount;
        transactionHistory.add(new Transaction(
            Transaction.Type.WITHDRAWAL, amount, balance, "Withdrawal"
        ));
        return this;
    }

    /**
     * Transfer money to another BankAccount.
     * Both accounts must not be frozen.
     */
    public void transferTo(BankAccount target, double amount)
            throws InsufficientFundsException {
        if (target == null) {
            throw new IllegalArgumentException("Target account cannot be null.");
        }
        if (this == target) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        // Deduct from this account
        validateNotFrozen();
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(amount, balance);
        }
        balance -= amount;
        transactionHistory.add(new Transaction(
            Transaction.Type.TRANSFER_OUT, amount, balance,
            "Transfer to " + target.getAccountId()
        ));

        // Add to target account
        target.receiveTransfer(amount, this.accountId);
    }

    /** Internal: receive incoming transfer */
    private void receiveTransfer(double amount, String fromAccountId) {
        validateNotFrozen();
        balance += amount;
        transactionHistory.add(new Transaction(
            Transaction.Type.TRANSFER_IN, amount, balance,
            "Transfer from " + fromAccountId
        ));
    }

    // ── Account Management ────────────────────────────────────────────────────

    public void freeze()   { this.frozen = true;  }
    public void unfreeze() { this.frozen = false; }

    private void validateNotFrozen() {
        if (frozen) {
            throw new IllegalStateException(
                "Account " + accountId + " is frozen. No transactions permitted."
            );
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getAccountId()     { return accountId; }
    public String  getAccountHolder() { return accountHolder; }
    public String  getAccountType()   { return accountType; }
    public double  getBalance()       { return balance; }
    public boolean isFrozen()         { return frozen; }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public int getTransactionCount() { return transactionHistory.size(); }

    // ── Overrides ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "BankAccount{id='%s', holder='%s', type='%s', balance=€%.2f, frozen=%s, transactions=%d}",
            accountId, accountHolder, accountType, balance, frozen, transactionHistory.size()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BankAccount)) return false;
        BankAccount other = (BankAccount) obj;
        return this.accountId.equals(other.accountId);
    }

    @Override
    public int hashCode() { return accountId.hashCode(); }

    // ── Demo main method ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  BankAccount OOP Demo — Aston Tellis");
        System.out.println("=".repeat(60));

        try {
            // Create accounts
            BankAccount current = new BankAccount("Aston Tellis", "CURRENT", 500.00);
            BankAccount savings = new BankAccount("Aston Tellis", "SAVINGS");

            System.out.println("\nAccounts created:");
            System.out.println("  " + current);
            System.out.println("  " + savings);

            // Method chaining
            current.deposit(1000.00).deposit(250.00);
            System.out.println("\nAfter deposits: €" + current.getBalance());

            // Withdraw
            current.withdraw(200.00);
            System.out.println("After withdrawal: €" + current.getBalance());

            // Transfer
            current.transferTo(savings, 500.00);
            System.out.println("\nAfter transfer:");
            System.out.println("  Current: €" + current.getBalance());
            System.out.println("  Savings: €" + savings.getBalance());

            // Print transaction history
            System.out.println("\nTransaction History (Current Account):");
            current.getTransactionHistory().forEach(t -> System.out.println("  " + t));

            // Demonstrate exception handling
            System.out.println("\nAttempting overdraft...");
            current.withdraw(99999.00);

        } catch (InsufficientFundsException e) {
            System.out.println("✗ Caught: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Demo complete.");
        System.out.println("=".repeat(60));
    }
}
