package revolut;

import java.math.BigDecimal;
import lombok.Data;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import reactor.core.publisher.Mono;
import revolut.models.Account;
import revolut.models.CreateAccountRequest;
import revolut.models.CreateDepositRequest;
import revolut.models.CreateTransferRequest;

public class AccountDAOImpl implements AccountDAO {

  private final Sql2o db;

  private final DataConfig config;

  public AccountDAOImpl(Sql2o db, DataConfig config) {
    this.db = db;
    this.config = config;
  }


  @Override
  public Mono<Account> createAccount(CreateAccountRequest obj) throws DataException {
    try (Connection conn = db.beginTransaction()) {
      Long newId = conn.createQuery(config.queries.createAccount, true)
          .addParameter("name", obj.getName())
          .executeUpdate()
          .getKeys(Long.class)
          .get(0);

      conn.commit(false);
      return findAccount(conn, newId);
    }
  }

  @Override
  public Mono<Account> findAccount(Long accountID) {
    try (Connection conn = db.beginTransaction()) {
      var acc = findAccount(conn, accountID);
      conn.commit();
      return acc;
    }
  }

  @Override
  public Mono<BigDecimal> createDeposit(CreateDepositRequest obj) throws DataException {
    Connection conn = db.beginTransaction();
    return findAccount(conn, obj.getAccountID())
        .flatMap(acc -> {
          Long from = null;
          Long to = obj.getAccountID();
          BigDecimal amount = obj.getAmount();
          Long newId = createAccTransaction(conn, from, to, amount);

          BigDecimal newBalance = acc.getBalance().add(amount);
          updateBalance(conn, acc.getId(), newBalance);

          conn.commit();
          return Mono.just(new Account(acc.getId(), acc.getName(), newBalance));
        })
        .doFinally(acc -> conn.close())
        .map(Account::getBalance)
        .switchIfEmpty(
            Mono.error(() -> new AccountNotFound("Account not found: " + obj.getAccountID())));
  }

  @Override
  public Mono<BigDecimal> createTransfer(CreateTransferRequest obj) throws DataException {
    if (obj.getFromAccount().equals(obj.getToAccount())) {
      return Mono.error(new CannotTransferSameAccount(
          "Cannot receive transfer on same account " + obj.getFromAccount()));
    }

    Connection conn = db.beginTransaction();

    return findAccount(conn, obj.getFromAccount())
        .zipWith(findAccount(conn, obj.getToAccount()))
        .flatMap(accs -> {
          var from = accs.getT1();
          var to = accs.getT2();

          BigDecimal amount = obj.getAmount();
          BigDecimal newFromBalance = from.getBalance().subtract(amount);
          BigDecimal newToBalance = to.getBalance().add(amount);

          if (newFromBalance.compareTo(BigDecimal.ZERO) < 0) {
            String message = String.format("Insufficient funds on account %s: %.2f",
                from.getId(), from.getBalance());
            return Mono.error(
                new InsufficientFunds(message));
          }
          Long newId = createAccTransaction(conn, from.getId(), to.getId(), amount);
          updateBalance(conn, from.getId(), newFromBalance);
          updateBalance(conn, to.getId(), newToBalance);
          conn.commit();
          return Mono.just(new Account(from.getId(), from.getName(), newFromBalance));
        })
        .doFinally(acc -> conn.close())
        .map(Account::getBalance)
        .switchIfEmpty(
            Mono.error(() -> {
              String message = String
                  .format("Invalid accounts: %s, %s", obj.getFromAccount(), obj.getToAccount());
              return new AccountNotFound(message);
            }));
  }

  private Mono<Account> findAccount(Connection connection, Long accountID) {
    return Mono.just(connection)
        .map(conn ->
            conn.createQuery(config.queries.findAccount)
                .addParameter("id", accountID)
                .executeAndFetchFirst(Account.class));
  }


  private Long createAccTransaction(Connection conn, Long from, Long to, BigDecimal amount) {
    return conn.createQuery(config.queries.createTransaction, true)
        .addParameter("from_acc", from)
        .addParameter("to_acc", to)
        .addParameter("amount", amount)
        .executeUpdate()
        .getKeys(Long.class)
        .get(0);
  }

  private void updateBalance(Connection conn, Long id, BigDecimal newBalance) {
    conn.createQuery(config.queries.updateBalance)
        .addParameter("id", id)
        .addParameter("balance", newBalance)
        .executeUpdate();
  }
}

class DataException extends RuntimeException {

  public DataException(String message) {
    super(message);
  }
}

class AccountNotFound extends DataException {

  public AccountNotFound(String message) {
    super(message);
  }
}

class InsufficientFunds extends DataException {

  public InsufficientFunds(String message) {
    super(message);
  }
}

class CannotTransferSameAccount extends DataException {

  public CannotTransferSameAccount(String message) {
    super(message);
  }
}

@Data
class DataConfig {

  public ConnectionConfig connection;
  public DaoConfigQueries queries;
}

@Data
class DaoConfigQueries {

  public String createAccount;
  public String findAccount;
  public String createTransaction;
  public String updateBalance;
}

@Data
class ConnectionConfig {

  public String url;
  public String user;
  public String password;
}