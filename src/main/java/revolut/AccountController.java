package revolut;

import com.google.gson.Gson;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import revolut.models.Account;
import revolut.models.CreateAccountRequest;
import revolut.models.CreateAccountResponse;
import revolut.models.CreateDepositRequest;
import revolut.models.CreateDepositResponse;
import revolut.models.CreateTransferRequest;
import revolut.models.CreateTransferResponse;

public class AccountController {

  private final AccountDAO accountDAO;

  public AccountController(AccountDAO accountDAO) {
    this.accountDAO = accountDAO;
  }

  public Mono<CreateAccountResponse> createAccount(CreateAccountRequest request) {
    return accountDAO.createAccount(request)
        .map(CreateAccountResponse::new);
  }

  public Mono<CreateDepositResponse> createDeposit(CreateDepositRequest request) {
    return accountDAO.createDeposit(request)
        .map(CreateDepositResponse::new);
  }

  public Mono<CreateTransferResponse> createTransfer(CreateTransferRequest request) {
    return accountDAO.createTransfer(request)
        .map(CreateTransferResponse::new);
  }

  public Mono<Account> getAccount(Long id) {
    return accountDAO.findAccount(id);
  }
}
