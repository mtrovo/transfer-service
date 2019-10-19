package revolut;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.sql2o.Sql2o;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;
import reactor.test.StepVerifier;
import revolut.models.Account;
import revolut.models.ConnectionConfig;
import revolut.models.CreateAccountRequest;
import revolut.models.CreateAccountResponse;
import revolut.models.CreateDepositRequest;
import revolut.models.CreateDepositResponse;
import revolut.models.CreateTransferRequest;
import revolut.models.CreateTransferResponse;
import revolut.models.DataConfig;


public class IntegrationTest {

  private static DisposableServer server;
  private static App app;
  private static String urlPrefix;
  private Gson gson = new Gson();

  @BeforeClass
  public static void setUp() throws Exception {
    DataConfig config = App.createDataConfig();
    Sql2o db = App.createDB(config.connection);

    app = new App(new AccountController(new AccountDAOImpl(db, config)));
    server = app.bindNow();

    urlPrefix = String.format("http://%s:%d", server.host(), server.port());
  }

  @Test
  public void createAccount() {
    Flux<String> response = post("/accounts",
        new CreateAccountRequest("TEST ACC"),
        CreateAccountResponse.class)
        .map(acc -> acc.getAccount().getName());

    StepVerifier.create(response)
        .expectNext("TEST ACC")
        .expectComplete()
        .verify(Duration.ofSeconds(1));
  }

  @Test
  public void createDeposit() {
    Flux<CreateAccountResponse> createAccount = post("/accounts",
        new CreateAccountRequest("TEST ACC"),
        CreateAccountResponse.class);

    StepVerifier.create(createAccount
        .flatMap(acc -> {
          Long id = acc.getAccount().getId();
          System.out.println("Depositing to acc " + id);
          return post(String.format("/accounts/%d/deposits", id),
              new CreateDepositRequest(id, new BigDecimal("99.99")),
              CreateDepositResponse.class);
        }))
        .expectNext(new CreateDepositResponse(new BigDecimal("99.99")))
        .expectComplete()
        .verify(Duration.ofSeconds(1));
  }

  @Test
  public void createDepositInvalidInput() {
    Flux<CreateAccountResponse> createAccount = post("/accounts",
        new CreateAccountRequest("TEST ACC"),
        CreateAccountResponse.class);

    StepVerifier.create(createAccount
        .flatMap(acc -> {
          Long id = acc.getAccount().getId();
          System.out.println("Depositing to acc " + id);
          return post(String.format("/accounts/%d/deposits", id),
              Map.of("amount", "99abc"),
              APIError.class);
        }))
        .expectNext(new APIError("Invalid input"))
        .expectComplete()
        .verify(Duration.ofSeconds(1));
  }

  @Test
  public void createTransfer() {
    var accA = post("/accounts",
        new CreateAccountRequest("TEST ACC A"),
        CreateAccountResponse.class)
        .map(CreateAccountResponse::getAccount)
        .blockFirst();
    var accB = post("/accounts",
        new CreateAccountRequest("TEST ACC B"),
        CreateAccountResponse.class)
        .map(CreateAccountResponse::getAccount)
        .blockFirst();

    var transfer = post(
            String.format("/accounts/%d/deposits", accA.getId()),
            new CreateDepositRequest(accA.getId(), new BigDecimal("99.99")),
            CreateDepositResponse.class)
        .flatMap(ign ->
            post(String.format("/accounts/%d/transfers", accA.getId()),
                new CreateTransferRequest(accA.getId(), accB.getId(), new BigDecimal("94.99")),
                CreateTransferResponse.class)
        );

    StepVerifier.create(transfer)
        .expectNext(new CreateTransferResponse(new BigDecimal("5.00")))
        .expectComplete()
        .verify(Duration.ofSeconds(1));

    StepVerifier.create(get("/accounts/" + accA.getId(), Account.class))
        .expectNext(new Account(accA.getId(), "TEST ACC A", new BigDecimal("5.00")))
        .verifyComplete();

    StepVerifier.create(get("/accounts/" + accB.getId(), Account.class))
        .expectNext(new Account(accB.getId(), "TEST ACC B", new BigDecimal("94.99")))
        .verifyComplete();

  }

  @Test
  public void createTransferNoFunds() {
    var accA = post("/accounts",
        new CreateAccountRequest("TEST ACC A"),
        CreateAccountResponse.class)
        .map(CreateAccountResponse::getAccount)
        .blockFirst();
    var accB = post("/accounts",
        new CreateAccountRequest("TEST ACC B"),
        CreateAccountResponse.class)
        .map(CreateAccountResponse::getAccount)
        .blockFirst();

    var transfer = post(
        String.format("/accounts/%d/deposits", accA.getId()),
        new CreateDepositRequest(accA.getId(), new BigDecimal("90.99")),
        CreateDepositResponse.class)
        .flatMap(ign ->
            post(String.format("/accounts/%d/transfers", accA.getId()),
                new CreateTransferRequest(accA.getId(), accB.getId(), new BigDecimal("94.99")),
                APIError.class)
        );

    StepVerifier.create(transfer)
        .expectNext(new APIError(String.format("Insufficient funds on account %d: 90,99", accA.getId())))
        .expectComplete()
        .verify(Duration.ofSeconds(1));

    StepVerifier.create(get("/accounts/" + accA.getId(), Account.class))
        .expectNext(new Account(accA.getId(), "TEST ACC A", new BigDecimal("90.99")))
        .verifyComplete();

    StepVerifier.create(get("/accounts/" + accB.getId(), Account.class))
        .expectNext(new Account(accB.getId(), "TEST ACC B", new BigDecimal("0.00")))
        .verifyComplete();

  }

  private <T> Flux<T> get(String uri, Class<T> respClass) {
    return HttpClient.create()
        .get()
        .uri(urlPrefix + uri)
        .response((resp, body) -> {
          System.out.println("Resp.status: " + resp.status());
          return body
              .aggregate()
              .asString()
              .map(str -> {
                if (resp.status().code() / 100 != 2) {
                  System.out.println("Resp error: " + str);
                }
                return gson.fromJson(str, respClass);
              });
        });
  }

  private <T> Flux<T> post(String uri, Object input, Class<T> respClass) {
    return HttpClient.create()
        .post()
        .uri(urlPrefix + uri)
        .send(toJson(input))
        .response((resp, body) -> {
          System.out.println("Resp.status: " + resp.status());
          return body
              .aggregate()
              .asString()
              .map(str -> {
                if (resp.status().code() / 100 != 2) {
                  System.out.println("Resp error: " + str);
                }
                return gson.fromJson(str, respClass);
              });
        });
  }

  private Publisher<ByteBuf> toJson(Object obj) {
    String data = gson.toJson(obj);
    return ByteBufFlux.fromString(Mono.just(data));
  }
}
