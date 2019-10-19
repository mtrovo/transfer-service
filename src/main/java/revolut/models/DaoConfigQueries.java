package revolut.models;

import lombok.Data;

@Data
public class DaoConfigQueries {

  public String createAccount;
  public String findAccount;
  public String createTransaction;
  public String updateBalance;
}
