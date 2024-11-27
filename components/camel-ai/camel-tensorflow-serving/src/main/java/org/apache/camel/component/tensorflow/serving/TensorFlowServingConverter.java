/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.tensorflow.serving;

import org.apache.camel.Converter;
import tensorflow.serving.Classification;
import tensorflow.serving.InputOuterClass;
import tensorflow.serving.RegressionOuterClass;

/**
 * Converter methods to convert from / to TensorFlow types.
 */
@Converter(generateLoader = true)
public class TensorFlowServingConverter {

    @Converter
    public static Classification.ClassificationRequest toClassificationRequest(InputOuterClass.Input input) {
        return Classification.ClassificationRequest.newBuilder().setInput(input).build();
    }

    @Converter
    public static RegressionOuterClass.RegressionRequest toRegressionRequest(InputOuterClass.Input input) {
        return RegressionOuterClass.RegressionRequest.newBuilder().setInput(input).build();
    }
}
