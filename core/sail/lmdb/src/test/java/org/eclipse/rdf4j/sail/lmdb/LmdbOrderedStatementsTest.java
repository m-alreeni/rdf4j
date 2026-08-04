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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbOrderedStatementsTest {

	@TempDir
	File dataDir;

	@Test
	void reportsSupportedOrdersAndComparator() throws Exception {
		LmdbStore store = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc,ospc"));
		store.init();
		try (NotifyingSailConnection connection = store.getConnection()) {
			Set<StatementOrder> supportedOrders = connection.getSupportedOrders(null, null, null);
			assertTrue(supportedOrders.contains(StatementOrder.S));
			assertTrue(supportedOrders.contains(StatementOrder.P));
			assertTrue(supportedOrders.contains(StatementOrder.O));
			assertFalse(supportedOrders.contains(StatementOrder.C));
			assertNotNull(connection.getComparator());
		} finally {
			store.shutDown();
		}
	}

	@Test
	void returnsOrderedStatementsAcrossContexts() throws Exception {
		LmdbStore store = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc,ospc"));
		store.init();
		try (NotifyingSailConnection connection = store.getConnection()) {
			connection.begin();
			connection.addStatement(Values.iri("urn:s3"), Values.iri("urn:p"), Values.literal("z"), Values.iri("urn:c1"));
			connection.addStatement(Values.iri("urn:s1"), Values.iri("urn:p"), Values.literal("x"), Values.iri("urn:c2"));
			connection.addStatement(Values.iri("urn:s2"), Values.iri("urn:p"), Values.literal("y"), Values.iri("urn:c1"));
			connection.commit();

			connection.begin(IsolationLevels.NONE);
			List<? extends Statement> ordered;
			try (CloseableIteration<? extends Statement> statements = connection.getStatements(StatementOrder.S, null,
					null, null, true, Values.iri("urn:c1"), Values.iri("urn:c2"))) {
				ordered = Iterations.asList(statements);
			}
			connection.commit();

			Comparator<Value> comparator = connection.getComparator();
			assertFalse(ordered.isEmpty());
			for (int i = 1; i < ordered.size(); i++) {
				Value previous = ordered.get(i - 1).getSubject();
				Value current = ordered.get(i).getSubject();
				assertTrue(comparator.compare(previous, current) <= 0);
			}
		} finally {
			store.shutDown();
		}
	}

	@Test
	void requestingUnsupportedOrderThrows() throws Exception {
		LmdbStore store = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc,ospc"));
		store.init();
		try (NotifyingSailConnection connection = store.getConnection()) {
			assertFalse(connection.getSupportedOrders(null, null, null).contains(StatementOrder.C));
			assertThrows(SailException.class, () -> connection.getStatements(StatementOrder.C, null, null, null, true));
		} finally {
			store.shutDown();
		}
	}
}
