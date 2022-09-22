/*
 * Copyright BigchainDB GmbH and BigchainDB contributors
 * SPDX-License-Identifier: (Apache-2.0 AND CC-BY-4.0)
 * Code is Apache-2.0 and docs are CC-BY-4.0
 */
package com.bigchaindb.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.TreeMap;



/**
 * The Class MetaData.
 */
public class MetaData {
	
	/** The id. */
	@SerializedName("id")
	private String id;
	
	/** The metadata. */
	@SerializedName("metadata")
	private Map<String, Object> metadata = new TreeMap<String, Object>();

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the metadata.
	 *
	 * @return the metadata
	 */
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetaData(String key, Object value) {
		this.metadata.put(key, value);
	}
}
