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
package org.eclipse.rdf4j.sail.base;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Comparator;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.junit.jupiter.api.Test;

class SailSourceConnectionOrderDelegationTest {

	@Test
	void delegatesSupportedOrdersAndComparatorToDataset() {
		Set<StatementOrder> expectedOrders = Set.of(StatementOrder.S, StatementOrder.O);
		Comparator<Value> expectedComparator = (left, right) -> 0;
		SailStore sailStore = new FixedOrderSailStore(expectedOrders, expectedComparator);
		Sail sail = createSail(sailStore);

		try (SailConnection connection = sail.getConnection()) {
			assertSame(expectedOrders, connection.getSupportedOrders(null, null, null));
			assertSame(expectedComparator, connection.getComparator());
		} finally {
			sail.shutDown();
		}
	}

	private static Sail createSail(SailStore sailStore) {
		return new AbstractNotifyingSail() {
			@Override
			protected void shutDownInternal() throws SailException {
				sailStore.close();
			}

			@Override
			protected NotifyingSailConnection getConnectionInternal() throws SailException {
				return new SailSourceConnection(this, sailStore, (FederatedServiceResolver) null) {
					@Override
					protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) {
					}

					@Override
					protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts) {
					}
				};
			}

			@Override
			public boolean isWritable() {
				return true;
			}

			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}
		};
	}

	private static final class FixedOrderSailStore implements SailStore {
		private final SailSource source;

		private FixedOrderSailStore(Set<StatementOrder> supportedOrders, Comparator<Value> comparator) {
			this.source = new FixedOrderSailSource(supportedOrders, comparator);
		}

		@Override
		public ValueFactory getValueFactory() {
			return SimpleValueFactory.getInstance();
		}

		@Override
		public EvaluationStatistics getEvaluationStatistics() {
			return new EvaluationStatistics();
		}

		@Override
		public SailSource getExplicitSailSource() {
			return source;
		}

		@Override
		public SailSource getInferredSailSource() {
			return source;
		}

		@Override
		public void close() {
		}
	}

	private static final class FixedOrderSailSource implements SailSource {
		private final SailDataset dataset;

		private FixedOrderSailSource(Set<StatementOrder> supportedOrders, Comparator<Value> comparator) {
			this.dataset = new FixedOrderSailDataset(supportedOrders, comparator);
		}

		@Override
		public SailSource fork() {
			return this;
		}

		@Override
		public SailSink sink(IsolationLevel level) {
			return new SailSink() {
				@Override
				public void prepare() {
				}

				@Override
				public void flush() {
				}

				@Override
				public void close() {
				}

				@Override
				public void setNamespace(String prefix, String name) {
				}

				@Override
				public void removeNamespace(String prefix) {
				}

				@Override
				public void clearNamespaces() {
				}

				@Override
				public void clear(Resource... contexts) {
				}

				@Override
				public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) {
				}

				@Override
				public void approve(Resource subj, IRI pred, Value obj, Resource ctx) {
				}

				@Override
				public void deprecate(Statement statement) {
				}
			};
		}

		@Override
		public SailDataset dataset(IsolationLevel level) {
			return dataset;
		}

		@Override
		public void prepare() {
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	}

	private static final class FixedOrderSailDataset implements SailDataset {
		private final Set<StatementOrder> supportedOrders;
		private final Comparator<Value> comparator;

		private FixedOrderSailDataset(Set<StatementOrder> supportedOrders, Comparator<Value> comparator) {
			this.supportedOrders = supportedOrders;
			this.comparator = comparator;
		}

		@Override
		public void close() {
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() {
			return new EmptyIteration<>();
		}

		@Override
		public String getNamespace(String prefix) {
			return null;
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return supportedOrders;
		}

		@Override
		public Comparator<Value> getComparator() {
			return comparator;
		}
	}
}
