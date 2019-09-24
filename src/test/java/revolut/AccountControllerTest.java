package revolut;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import revolut.models.Account;
import revolut.models.CreateAccountRequest;
import revolut.models.CreateAccountResponse;
import revolut.models.CreateDepositRequest;
import revolut.models.CreateDepositResponse;
import revolut.models.CreateTransferRequest;
import revolut.models.CreateTransferResponse;

public class AccountControllerTest {

  private AccountController controller;
  private AccountDAO accountDAO;


  @Before
  public void setUp() throws Exception {
    accountDAO = mock(AccountDAO.class, Answers.RETURNS_SMART_NULLS);
    controller = new AccountController(accountDAO);
  }

  @Test
  public void createAccount() {
    var req = new CreateAccountRequest("TEST");

    when(accountDAO.createAccount(req))
        .thenReturn(Mono.just(new Account(99L, "TEST", BigDecimal.ZERO)));

    var resp = controller.createAccount(req);
    StepVerifier.create(resp)
        .expectNext(new CreateAccountResponse(new Account(99L, "TEST", BigDecimal.ZERO)))
        .expectComplete()
        .verify();
  }

  @Test
  public void createDepositSuccess() {
    var req = new CreateDepositRequest(99L, BigDecimal.TEN);
    when(accountDAO.createDeposit(req))
        .thenReturn(Mono.just(new BigDecimal("25.00")));

    StepVerifier.create(controller.createDeposit(req))
        .expectNext(new CreateDepositResponse(new BigDecimal("25.00")))
        .expectComplete()
        .verify();
  }

  @Test
  public void createDepositAccountNotFound() {
    var req = new CreateDepositRequest(99L, BigDecimal.TEN);
    when(accountDAO.createDeposit(req))
        .thenReturn(Mono.error(new AccountNotFound("Account not found")));

    StepVerifier.create(controller.createDeposit(req))
        .expectErrorMessage("Account not found")
        .verify();
  }

  @Test
  public void createTransferSuccess() {
    var req = new CreateTransferRequest(10L, 99L, new BigDecimal("100.00"));

    when(accountDAO.createTransfer(req))
        .thenReturn(Mono.just(new BigDecimal("900.00")));

    StepVerifier.create(controller.createTransfer(req))
        .expectNext(new CreateTransferResponse(new BigDecimal("900.00")))
        .expectComplete()
        .verify();
  }

  @Test
  public void createTransferNoFunds() {
    var req = new CreateTransferRequest(10L, 99L, new BigDecimal("100.00"));

    when(accountDAO.createTransfer(req))
        .thenReturn(Mono.error(new InsufficientFunds("insufficient funds for transaction")));

    StepVerifier.create(controller.createTransfer(req))
        .expectErrorMessage("insufficient funds for transaction")
        .verify();
  }

  @Test
  public void createTransferAccountNotFound() {
    var req = new CreateTransferRequest(10L, 99L, new BigDecimal("100.00"));

    when(accountDAO.createTransfer(req))
        .thenReturn(Mono.error(new AccountNotFound("Account not found")));

    StepVerifier.create(controller.createTransfer(req))
        .expectErrorMessage("Account not found")
        .verify();
  }
}