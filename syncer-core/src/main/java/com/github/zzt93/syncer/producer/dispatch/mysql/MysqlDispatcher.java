package com.github.zzt93.syncer.producer.dispatch.mysql;

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.zzt93.syncer.common.Filter.FilterRes;
import com.github.zzt93.syncer.common.LogbackLoggingField;
import com.github.zzt93.syncer.common.data.BinlogDataId;
import com.github.zzt93.syncer.common.data.DataId;
import com.github.zzt93.syncer.common.util.FallBackPolicy;
import com.github.zzt93.syncer.config.common.InvalidConfigException;
import com.github.zzt93.syncer.data.SimpleEventType;
import com.github.zzt93.syncer.producer.dispatch.Dispatcher;
import com.github.zzt93.syncer.producer.input.mysql.AlterMeta;
import com.github.zzt93.syncer.producer.input.mysql.connect.BinlogInfo;
import com.github.zzt93.syncer.producer.input.mysql.meta.ConsumerSchemaMeta;
import com.github.zzt93.syncer.producer.input.mysql.meta.TableMeta;
import com.github.zzt93.syncer.producer.output.ProducerSink;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zzt
 */
public class MysqlDispatcher implements Dispatcher {

  private final List<ConsumerChannel> consumerChannels;
  private final AtomicReference<BinlogInfo> binlogInfo;
  private final Logger logger = LoggerFactory.getLogger(MysqlDispatcher.class);

  public MysqlDispatcher(HashMap<ConsumerSchemaMeta, ProducerSink> sinkHashMap,
                         AtomicReference<BinlogInfo> binlogInfo, boolean onlyUpdated) {
    consumerChannels = new ArrayList<>(sinkHashMap.size());
    this.binlogInfo = binlogInfo;
    if (sinkHashMap.isEmpty()) {
      logger.error("No dispatch info fetched: no meta info dispatcher & output sink");
      throw new InvalidConfigException("Invalid address & schema & table config");
    }
    for (Entry<ConsumerSchemaMeta, ProducerSink> entry : sinkHashMap.entrySet()) {
      logger.info("Listening {}, dispatch to {}", entry.getKey(), entry.getValue());
      consumerChannels.add(new ConsumerChannel(entry.getKey(), entry.getValue(), onlyUpdated));
    }
  }

  @Override
  public boolean dispatch(SimpleEventType simpleEventType, Object... data) {
    Preconditions.checkState(data.length == 2);
    Event[] events = new Event[]{(Event) data[0], (Event) data[1]};
    BinlogDataId dataId = DataId.fromEvent(events, binlogInfo.get().getBinlogFilename());
    MDC.put(LogbackLoggingField.EID, dataId.eventId());
    boolean res = true;
    for (ConsumerChannel consumerChannel : consumerChannels) {
      FilterRes decide = consumerChannel.decide(simpleEventType, dataId, events);
      res = res && FilterRes.ACCEPT == decide;
    }
    MDC.remove(LogbackLoggingField.EID);
    return res;
  }

  public void updateSchemaMeta(AlterMeta alterMeta) {
    List<ConsumerChannel> interested = new LinkedList<>();
    for (ConsumerChannel consumerChannel : consumerChannels) {
      if (consumerChannel.interestedSchemaMeta(alterMeta)) {
        interested.add(consumerChannel);
      }
    }
    if (interested.isEmpty()) {
      return;
    }

    logger.info("Detect alter table {}, may affect column index, re-syncing", alterMeta);
    TableMeta full = null;
    long sleepInSecond = 1;
    while (full == null) {
      try {
        full = ConsumerSchemaMeta.MetaDataBuilder.tableMeta(alterMeta.getConnection(), alterMeta.getSchema(), alterMeta.getTable());
      } catch (SQLException e) {
        logger.error("Fail to fetch meta info after alter", e);
        sleepInSecond = FallBackPolicy.POW_2.sleep(sleepInSecond);
      }
    }
    logger.info("Fetch related {}", full);
    for (ConsumerChannel consumerChannel : interested) {
      consumerChannel.updateSchemaMeta(alterMeta, full);
    }
  }



}
