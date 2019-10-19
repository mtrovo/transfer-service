package revolut.models;

import lombok.Data;

@Data
public class DataConfig {

  public ConnectionConfig connection;
  public DaoConfigQueries queries;
}

