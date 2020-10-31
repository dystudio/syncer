package com.github.zzt93.syncer.stat.vo;

import com.github.zzt93.syncer.common.data.SyncInitMeta;

import java.util.List;

public class ConsumerStatVO {

	private final String consumerId;
	private final long synced;
	private final List<SyncInitMeta> position;
	private final long filterQueue;

	public ConsumerStatVO(String consumerId, long synced, List<SyncInitMeta> position, long filterQueue) {
		this.consumerId = consumerId;
		this.synced = synced;
		this.position = position;
		this.filterQueue = filterQueue;
	}
}
