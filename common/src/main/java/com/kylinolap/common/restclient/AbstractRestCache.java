/*
 * Copyright 2013-2014 eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kylinolap.common.restclient;

/**
 * @author xjiang
 * 
 */
public abstract class AbstractRestCache<K, V> {

    protected final Broadcaster.TYPE syncType;

    protected AbstractRestCache(Broadcaster.TYPE syncType) {
        this.syncType = syncType;
    }

    protected final void syncRemote(Object key, Broadcaster.EVENT syncAction) {
        String syncKey = (syncType == Broadcaster.TYPE.METADATA) ? "metadata" : key.toString();
        Broadcaster.queue(syncType.getType(), syncAction.getType(), syncKey);
    }

    public abstract void put(K key, V value);

    public abstract void putLocal(K key, V value);

    public abstract void remove(K key);

    public abstract void removeLocal(K key);

    public abstract void clear();

    public abstract int size();
}
