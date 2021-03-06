package com.github.zzt93.syncer.consumer.filter.impl;

import com.github.zzt93.syncer.common.data.SyncData;
import com.github.zzt93.syncer.config.common.InvalidConfigException;
import com.github.zzt93.syncer.config.consumer.filter.FilterConfig;
import com.github.zzt93.syncer.config.consumer.filter.Switcher;
import com.github.zzt93.syncer.consumer.filter.ConditionalStatement;
import com.github.zzt93.syncer.data.util.SyncFilter;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzt
 */
public class Switch implements ConditionalStatement {

  private final static Gson gson = new Gson();
  private final Logger logger = LoggerFactory.getLogger(Switch.class);
  private final SwitchCondition switchCondition;
  private final Map<String, List<SyncFilter>> actionsMap;

  public Switch(SpelExpressionParser parser, Switcher filter) {
    switchCondition = new SwitchCondition(filter.getSwitch(), parser);
    Map<String, List<SyncFilter>> tmp = new HashMap<>();
    filter.getCase()
        .forEach((k, v) -> {
          if (v == null) {
            throw new InvalidConfigException("Unsupport empty case: " + k);
          }
          tmp.put(k, v.stream()
              .map(c -> gson.fromJson(gson.toJsonTree(c), FilterConfig.class).toFilter(parser))
              .collect(Collectors.toList()));
        });
    actionsMap = Collections.unmodifiableMap(tmp);
  }

  @Override
  public List<SyncFilter> conditional(SyncData syncData) {
    StandardEvaluationContext context = syncData.getContext();
    String conditionRes = switchCondition.execute(context);
    if (conditionRes == null) {
      logger.error("switch on `null`, skip {}", syncData);
      return null;
    }
    List<SyncFilter> caseClause = actionsMap.get(conditionRes);
    if (caseClause == null &&
        actionsMap.containsKey(Switcher.DEFAULT)) {
      caseClause = actionsMap.get(Switcher.DEFAULT);
    }
    if (caseClause == null) {
      logger.error("Unknown switch result and no default config : {}", conditionRes);
      return null;
    }
    return caseClause;
  }

}
