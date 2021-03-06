/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.service;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.step.DefaultStepReporter;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrei Varabyeu
 */
public abstract class Launch {
	private static final ThreadLocal<Launch> CURRENT_LAUNCH = new InheritableThreadLocal<>();

	static final Logger LOGGER = LoggerFactory.getLogger(Launch.class);

	private final ListenerParameters parameters;

	private final StepReporter stepReporter;

	Launch(ListenerParameters parameters, StepReporter reporter) {
		this.parameters = parameters;
		stepReporter = reporter;
		CURRENT_LAUNCH.set(this);
	}

	Launch(ListenerParameters parameters) {
		this.parameters = parameters;
		stepReporter = new DefaultStepReporter(this);
		CURRENT_LAUNCH.set(this);
	}

	abstract public Maybe<String> start();

	/**
	 * Finishes launch in ReportPortal. Blocks until all items are reported correctly
	 *
	 * @param rq Finish RQ
	 */
	abstract public void finish(final FinishExecutionRQ rq);

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq Start RQ
	 * @return Test Item ID promise
	 */
	abstract public Maybe<String> startTestItem(final StartTestItemRQ rq);

	/**
	 * Starts new test item in ReportPortal asynchronously (non-blocking)
	 *
	 * @param rq       Start RQ
	 * @param parentId Parent ID
	 * @return Test Item ID promise
	 */
	abstract public Maybe<String> startTestItem(final Maybe<String> parentId, final StartTestItemRQ rq);

	/**
	 * Starts new test item in ReportPortal in respect of provided retry
	 *
	 * @param parentId promise of ID of parent
	 * @param retryOf  promise of ID of retried element
	 * @param rq       promise of ID of request
	 * @return Promise of Test Item ID
	 */
	abstract public Maybe<String> startTestItem(final Maybe<String> parentId, final Maybe<String> retryOf, final StartTestItemRQ rq);

	/**
	 * Finishes Test Item in ReportPortal. Non-blocking. Schedules finish after success of all child items
	 *
	 * @param itemId Item ID promise
	 * @param rq     Finish request
	 * @return Promise of Test Item finish response
	 */
	abstract public Maybe<OperationCompletionRS> finishTestItem(Maybe<String> itemId, final FinishTestItemRQ rq);

	public ListenerParameters getParameters() {
		// Sticking any thread which makes this call to the current Launch to be able to use Step Reporter and other methods
		CURRENT_LAUNCH.set(this);
		return this.parameters;
	}

	/**
	 * Returns a current launch in a link to the current thread.
	 *
	 * @return launch instance
	 */
	public static Launch currentLaunch() {
		return CURRENT_LAUNCH.get();
	}

	/**
	 * Returns Nested Step reporter for the current launch.
	 *
	 * @return a {@link StepReporter} instance
	 */
	public StepReporter getStepReporter() {
		return stepReporter;
	}

	/**
	 * Implementation for disabled Reporting
	 */
	public static final Launch NOOP_LAUNCH = new Launch(new ListenerParameters(), StepReporter.NOOP_STEP_REPORTER) {

		@Override
		public Maybe<String> start() {
			return Maybe.empty();
		}

		@Override
		public void finish(FinishExecutionRQ rq) {

		}

		@Override
		public Maybe<String> startTestItem(StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		public Maybe<String> startTestItem(Maybe<String> parentId, StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		public Maybe<String> startTestItem(Maybe<String> parentId, Maybe<String> retryOf, StartTestItemRQ rq) {
			return Maybe.empty();
		}

		@Override
		public Maybe<OperationCompletionRS> finishTestItem(Maybe<String> itemId, FinishTestItemRQ rq) {
			return Maybe.empty();
		}
	};
}
