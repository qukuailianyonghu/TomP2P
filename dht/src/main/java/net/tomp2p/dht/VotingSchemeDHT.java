/*
 * Copyright 2009 Thomas Bocek
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.tomp2p.dht;

import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.DigestResult;
import net.tomp2p.storage.Data;

public class VotingSchemeDHT implements EvaluationSchemeDHT {
    
    private static final NavigableMap<Number640, Collection<Number160>> emptyMap = new TreeMap<Number640, Collection<Number160>>();
    
    @Override
    public Collection<Number640> evaluate1(Map<PeerAddress, Map<Number640, Number160>> rawKeys) {
        Map<Number640, Integer> counter = new HashMap<Number640, Integer>();
        Set<Number640> result = new HashSet<Number640>();

        int size = rawKeys == null ? 0 : rawKeys.size();
        int majority = (size + 1) / 2;

        if (rawKeys != null) {
            for (PeerAddress address : rawKeys.keySet()) {
                Collection<Number640> num640 = rawKeys.get(address).keySet();
                if (num640 != null) {
                    for (Number640 key : num640) {
                        int c = 1;
                        Integer count = counter.get(key);
                        if (count != null)
                            c = count + 1;
                        counter.put(key, c);
                        if (c >= majority)
                            result.add(key);
                    }
                }
            }
        }

        return result;
    }
    
    @Override
    public Map<Number640, Data> evaluate2(final Map<PeerAddress, Map<Number640, Data>> rawData) {
        if (rawData == null) {
            throw new IllegalArgumentException("cannot evaluate, as no result provided");
        }
        Map<Number160, Integer> counter = new HashMap<Number160, Integer>();
        Map<Number640, Data> result = new HashMap<Number640, Data>();
        int size = rawData.size();
        int majority = (size + 1) / 2;
        for (PeerAddress address : rawData.keySet()) {
            Map<Number640, Data> data = rawData.get(address);
            for (Number640 contentKey : data.keySet()) {
                Data dat = data.get(contentKey);
                Number160 hash = dat.hash().xor(contentKey.contentKey()).
                        xor(contentKey.domainKey()).xor(contentKey.locationKey());
                int c = 1;
                Integer count = counter.get(hash);
                if (count != null) {
                    c = count + 1;
                }
                counter.put(hash, c);
                if (c >= majority) {
                    result.put(contentKey, dat);
                }
            }
        }
        return result;
    }

    @Override
    public Object evaluate3(Map<PeerAddress, Object> rawObjects) {
        return evaluate0(rawObjects);
    }

    @Override
    public ByteBuf evaluate4(Map<PeerAddress, ByteBuf> rawChannels) {
        return evaluate0(rawChannels);
    }

    @Override
    public DigestResult evaluate5(Map<PeerAddress, DigestResult> rawDigest) {
        DigestResult retVal =  evaluate0(rawDigest);
        //if its null, we know that we did not get any results. In order to return null, we return and empty digest result.
        return retVal == null ? new DigestResult(emptyMap):retVal;
    }

    @Override
	public Collection<Number640> evaluate6(Map<PeerAddress, Map<Number640, Byte>> rawKeys) {
	    Map<Number640, Integer> counter = new HashMap<Number640, Integer>();
	    Set<Number640> result = new HashSet<Number640>();
	
	    int size = rawKeys == null ? 0 : rawKeys.size();
	    int majority = (size + 1) / 2;
	
	    if (rawKeys != null) {
	        for (PeerAddress address : rawKeys.keySet()) {
	            Collection<Number640> num640 = rawKeys.get(address).keySet();
	            if (num640 != null) {
	                for (Number640 key : num640) {
	                    int c = 1;
	                    Integer count = counter.get(key);
	                    if (count != null)
	                        c = count + 1;
	                    counter.put(key, c);
	                    if (c >= majority)
	                        result.add(key);
	                }
	            }
	        }
	    }
	
	    return result;
	}

	private static <K> K evaluate0(Map<PeerAddress, K> raw) {
        if (raw == null)
            throw new IllegalArgumentException(
                    "cannot evaluate, as no result provided. Most likely you are not using direct messages, but rather put() or add(). For put and add, you have to use getData()");
        Map<K, Integer> counter = new HashMap<K, Integer>();
        K best = null;
        int count = 0;
        for (PeerAddress address : raw.keySet()) {
            K k = raw.get(address);
            if (k != null) {
                Integer c = counter.get(k);
                if (c == null)
                    c = 0;
                c++;
                counter.put(k, c);
                if (c > count) {
                    best = k;
                    count = c;
                }
            }
        }
        return best;
    }
}
