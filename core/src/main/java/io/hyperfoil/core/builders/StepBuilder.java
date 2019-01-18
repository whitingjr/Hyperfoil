/*
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.hyperfoil.core.builders;

import java.util.List;
import java.util.function.Consumer;

import io.hyperfoil.api.config.Sequence;
import io.hyperfoil.api.config.ServiceLoadedBuilder;
import io.hyperfoil.api.config.Step;
import io.hyperfoil.function.SerializableSupplier;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public interface StepBuilder {
   List<Step> build(SerializableSupplier<Sequence> sequence);

   default <T extends StepBuilder> void forEach(Class<T> type, Consumer<T> consumer) {
      if (type.isInstance(this)) {
         consumer.accept((T) this);
      }
   }

   interface Factory extends ServiceLoadedBuilder.Factory<StepBuilder> {}
}
