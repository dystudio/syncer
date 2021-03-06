package com.github.zzt93.syncer.producer.input.mysql;

import com.github.zzt93.syncer.config.common.MysqlConnection;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class AlterMeta {
  private MysqlConnection connection;
  private final String schema;
  private final String table;

  public AlterMeta(String schema, String table) {
    this.schema = schema;
    this.table = table;
  }

  public AlterMeta setConnection(MysqlConnection connection) {
    this.connection = connection;
    return this;
  }
}
