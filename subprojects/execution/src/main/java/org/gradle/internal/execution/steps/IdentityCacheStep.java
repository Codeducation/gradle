/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.execution.steps;

import org.gradle.cache.Cache;
import org.gradle.internal.Cast;
import org.gradle.internal.Try;
import org.gradle.internal.execution.DeferredExecutionAwareStep;
import org.gradle.internal.execution.DeferredResultProcessor;
import org.gradle.internal.execution.IdentityContext;
import org.gradle.internal.execution.Result;
import org.gradle.internal.execution.Step;
import org.gradle.internal.execution.UnitOfWork;
import org.gradle.internal.execution.UnitOfWork.Identity;

public class IdentityCacheStep<C extends IdentityContext, R extends Result> implements DeferredExecutionAwareStep<C, R> {

    private final Step<? super IdentityContext, ? extends R> delegate;

    public IdentityCacheStep(Step<? super IdentityContext, ? extends R> delegate) {
        this.delegate = delegate;
    }

    @Override
    public R execute(UnitOfWork work, C context) {
        return delegate.execute(work, context);
    }

    @Override
    public <T, O> T executeDeferred(UnitOfWork work, C context, Cache<Identity, Try<O>> cache, DeferredResultProcessor<O, T> processor) {
        Identity identity = context.getIdentity();
        Try<O> cachedOutput = cache.get(identity);
        if (cachedOutput != null) {
            return processor.processCachedOutput(cachedOutput);
        } else {
            return processor.processDeferredOutput(() -> cache.get(
                identity,
                () -> execute(work, context).getExecutionResult()
                    .map(Result.ExecutionResult::getOutput)
                    .map(Cast::<O>uncheckedNonnullCast)));
        }
    }
}
