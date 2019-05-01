/**
 * Copyright (C) 2019 Instacount Inc. (developers@instacount.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.google.appengine.api.memcache;

import com.googlecode.objectify.cache.IdentifiableValue;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.cache.spymemcached.SpyIdentifiableValue;

import net.spy.memcached.CASValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper to use google's MemecacheService with Objectify
 *
 * This class is in google's appengine package to use a package private class
 *
 * @author Mohammad Sarhan
 */
public class LocalMemcacheService implements MemcacheService {

	private com.google.appengine.api.memcache.MemcacheService memcacheService;

	public LocalMemcacheService(com.google.appengine.api.memcache.MemcacheService memcacheService)
	{
		this.memcacheService = memcacheService;
	}

	@Override
	public Object get(String s)
	{
		return memcacheService.get(s);
	}

	@Override
	public Map<String, IdentifiableValue> getIdentifiables(Collection<String> collection)
	{

		Map<String, com.google.appengine.api.memcache.MemcacheService.IdentifiableValue> values =
				memcacheService.getIdentifiables(collection);

		Map<String, IdentifiableValue> transformed = new HashMap<>(values.size());

		for (Map.Entry<String, com.google.appengine.api.memcache.MemcacheService.IdentifiableValue> entry : values.entrySet())
		{
			long cas = ((AsyncMemcacheServiceImpl.IdentifiableValueImpl) entry.getValue()).getCasId();
			Object value = entry.getValue().getValue();

			transformed.put(entry.getKey(),
					new SpyIdentifiableValue(
							new CASValue<>(cas, value)

					));
		}

		return transformed;
	}

	@Override
	public Map<String, Object> getAll(Collection<String> collection)
	{
		return memcacheService.getAll(collection);
	}

	@Override
	public void put(String s, Object o)
	{
		memcacheService.put(s, o);
	}

	@Override
	public void putAll(Map<String, Object> map)
	{
		memcacheService.putAll(map);
	}

	@Override
	public Set<String> putIfUntouched(Map<String, CasPut> map)
	{

		Map<String, com.google.appengine.api.memcache.MemcacheService.CasValues> transformed =
				new HashMap<>(map.size());

		for (Map.Entry<String, CasPut> entry : map.entrySet())
		{
			CasPut put = entry.getValue();
			long cas = ((SpyIdentifiableValue) put.getIv()).getCasValue().getCas();
			Object oldVal = put.getIv().getValue();
			Object newVal = put.getNextToStore();

			transformed.put(entry.getKey(),
					new com.google.appengine.api.memcache.MemcacheService.CasValues(
							new AsyncMemcacheServiceImpl.IdentifiableValueImpl(oldVal, cas), newVal));
		}

		return memcacheService.putIfUntouched(transformed);
	}

	@Override
	public void deleteAll(Collection<String> collection)
	{
		memcacheService.deleteAll(collection);
	}
}
