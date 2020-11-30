package com.github.zzt93.syncer.common.data;

import com.github.zzt93.syncer.config.common.InvalidConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * ----------- index/insert by query ------------
 * @see SyncByQuery
 */
public class ExtraQuery implements com.github.zzt93.syncer.data.ExtraQuery {

  private static final Logger logger = LoggerFactory.getLogger(ExtraQuery.class);
  private final HashMap<String, Object> queryBy = new HashMap<>();
  private final transient SyncData data;
  private String queryId;
  private String indexName;
  private String typeName;
  private String[] select;
  private String[] as;
  private final HashMap<String, Object> queryResult = new HashMap<>();

  ExtraQuery(SyncData data) {
    this.data = data;
  }

  public String getTypeName() {
    return typeName;
  }

  public ExtraQuery setTypeName(String typeName) {
    this.typeName = typeName;
    return this;
  }

  public ExtraQuery filter(String field, Object value) {
    queryBy.put(field, value);
    return this;
  }

  public ExtraQuery select(String... field) {
    select = field;
    for (String col : field) {
      data.getFields().put(col, new ExtraQueryField(this, col));
    }
    return this;
  }

  public ExtraQuery addField(String... cols) {
    if (cols.length != select.length) {
      throw new InvalidConfigException("Column length is not same as query select result");
    }
    this.as = cols;
    for (String col : cols) {
      data.getFields().put(col, new ExtraQueryField(this, col));
    }
    return this;
  }

  public String getIndexName() {
    return indexName;
  }

  public ExtraQuery setIndexName(String indexName) {
    this.indexName = indexName;
    return this;
  }

  public HashMap<String, Object> getQueryBy() {
    return queryBy;
  }

  public String[] getSelect() {
    return select;
  }

  public String getAs(int i) {
    return as != null ? as[i] : select[i];
  }

  public void addQueryResult(Map<String, Object> result) {
    queryResult.putAll(result);
  }

  public Object getQueryResult(String key) {
    Object o = queryResult.get(key);
    if (o == null) {
      logger.warn("Fail to query [{}] by {}", key, this);
    }
    return o;
  }

  public Object getField(String s) {
    return data.getField(s);
  }

  @Override
  public String toString() {
    return "ExtraQuery{select " + Arrays.toString(select) + " as " + Arrays.toString(as)
        + " from " + indexName + "." + typeName + " where " + queryBy +"}" + (!queryResult.isEmpty() ? queryResult : "");
  }
}
