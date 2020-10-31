package com.github.zzt93.syncer.config.syncer;

import com.github.zzt93.syncer.config.common.EtcdConnection;
import com.github.zzt93.syncer.config.common.InvalidConfigException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zzt
 */
@Setter
@Getter
public class SyncerConfig {

  public static final String SERVER_PORT = "port";
  public static final String INSTANCE = "instance";
  public static final String DEFAULT_START = "40000";
  private static final Logger logger = LoggerFactory.getLogger(SyncerConfig.class);
  private static final String RETRY = "10";

  private int port;
  private SyncerAck ack;
  private SyncerInput input;
  private SyncerFilter filter;
  private SyncerOutput output;


  public void setPort(int port) {
    if (port <= 0 || port > 65535) throw new InvalidConfigException("Invalid port config " + port);
    this.port = port;
  }

  public EtcdConnection getEtcd() {
    return ack.getEtcd();
  }

  public boolean hasEtcd() {
    return ack.hasEtcd();
  }
}
