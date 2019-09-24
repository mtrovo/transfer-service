# transaction-service

Test project that handles transactions using Reactor project.

This project uses Reactor with reactor-netty for HTTP requests,
handling as far as possible the request in asynchronous mode.


## API
### POST /accounts
Create an account.
```
{"name": "John Smith"}
```

### GET /accounts/:id
Get account details, if it exists.

### POST /accounts/:id/deposits
Create a deposit to an account.
```
{"amount":"19.00"}
```

### POST /accounts/:fromAccount/transfers
Create transfer from `fromAccount` to `toAccount`.
```
{"amount": "41.99", "toAccount": 99}
```

## Running
The project is built using gradlew, running on JDK 12.
To run the project run:
```
./gradlew run
```

This should install all dependencies and run the project on `localhost:8080`.

