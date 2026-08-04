/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

final class LmdbValueIdComparator implements Comparator<Value> {

	private final ValueStore valueStore;
	private final Comparator<Value> fallbackComparator;

	LmdbValueIdComparator(ValueStore valueStore, Comparator<Value> fallbackComparator) {
		this.valueStore = valueStore;
		this.fallbackComparator = fallbackComparator;
	}

	@Override
	public int compare(Value left, Value right) {
		if (left == right) {
			return 0;
		}

		long leftId = getOrResolveId(left);
		long rightId = getOrResolveId(right);
		if (leftId != LmdbValue.UNKNOWN_ID && rightId != LmdbValue.UNKNOWN_ID) {
			return Long.compareUnsigned(leftId, rightId);
		}

		return fallbackComparator.compare(left, right);
	}

	private static long getKnownId(Value value) {
		if (value instanceof LmdbValue) {
			return ((LmdbValue) value).getInternalID();
		}
		return LmdbValue.UNKNOWN_ID;
	}

	private long getOrResolveId(Value value) {
		long knownId = getKnownId(value);
		if (knownId != LmdbValue.UNKNOWN_ID || value == null) {
			return knownId;
		}
		try {
			return valueStore.getId(value);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
