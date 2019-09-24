package revolut;

import java.math.BigDecimal;
import reactor.core.publisher.Mono;
import revolut.models.Account;
import revolut.models.CreateAccountRequest;
import revolut.models.CreateDepositRequest;
import revolut.models.CreateTransferRequest;

public interface AccountDAO {

  Mono<Account> createAccount(CreateAccountRequest obj) throws DataException;

  Mono<Account> findAccount(Long accountID);

  Mono<BigDecimal> createDeposit(CreateDepositRequest obj) throws DataException;

  Mono<BigDecimal> createTransfer(CreateTransferRequest obj) throws DataException;
}
