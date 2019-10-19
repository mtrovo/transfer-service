package revolut.models;

import lombok.Data;

@Data
public class ConnectionConfig {

  public String url;
  public String user;
  public String password;
}
