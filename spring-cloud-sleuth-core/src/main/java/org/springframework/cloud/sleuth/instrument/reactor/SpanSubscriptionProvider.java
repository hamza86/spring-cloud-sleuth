/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.reactor;

import java.util.function.Supplier;

import brave.Span;
import brave.Tracing;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.BeanFactory;
import reactor.util.context.Context;

/**
 * Supplier to lazily start a {@link SpanSubscription}.
 *
 * @param <T> type of returned subscription
 * @author Marcin Grzejszczak
 */
final class SpanSubscriptionProvider<T> implements Supplier<SpanSubscription<T>> {

	private static final Log log = LogFactory.getLog(SpanSubscriptionProvider.class);

	final BeanFactory beanFactory;

	final Subscriber<? super T> subscriber;

	final Context context;

	final String name;

	private volatile Tracing tracing;

	SpanSubscriptionProvider(BeanFactory beanFactory, Subscriber<? super T> subscriber,
			Context context, String name) {
		this.beanFactory = beanFactory;
		this.subscriber = subscriber;
		this.context = context;
		this.name = name;
		if (log.isTraceEnabled()) {
			log.trace("Spring context [" + beanFactory + "], Reactor context [" + context
					+ "], name [" + name + "]");
		}
	}

	@Override
	public SpanSubscription<T> get() {
		return newCoreSubscriber(tracing());
	}

	SpanSubscription<T> newCoreSubscriber(Tracing tracing) {
		Span root = this.context.hasKey(Span.class) ? this.context.get(Span.class)
				: tracing.tracer().currentSpan();
		return new ScopePassingSpanSubscriber<>(this.subscriber, this.context, tracing,
				root);
	}

	private Tracing tracing() {
		if (this.tracing == null) {
			this.tracing = this.beanFactory.getBean(Tracing.class);
		}
		return this.tracing;
	}

}
