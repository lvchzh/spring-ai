/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.embedding.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.observation.AiOperationMetadata;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.HighCardinalityKeyNames;
import static org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.LowCardinalityKeyNames;

/*
 * Unit tests for {@link DefaultEmbeddingModelObservationConvention}.
 *
 * @author Thomas Vitale
 */
class DefaultEmbeddingModelObservationConventionTests {

	private final DefaultEmbeddingModelObservationConvention observationConvention = new DefaultEmbeddingModelObservationConvention();

	@Test
	void shouldHaveName() {
		assertThat(this.observationConvention.getName())
			.isEqualTo(DefaultEmbeddingModelObservationConvention.DEFAULT_NAME);
	}

	@Test
	void shouldHaveContextualName() {
		EmbeddingModelObservationContext observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(generateEmbeddingRequest())
			.operationMetadata(generateOperationMetadata())
			.requestOptions(EmbeddingModelRequestOptions.builder().model("mistral").build())
			.build();
		assertThat(this.observationConvention.getContextualName(observationContext)).isEqualTo("embedding mistral");
	}

	@Test
	void supportsOnlyEmbeddingModelObservationContext() {
		EmbeddingModelObservationContext observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(generateEmbeddingRequest())
			.operationMetadata(generateOperationMetadata())
			.requestOptions(EmbeddingModelRequestOptions.builder().model("supermodel").build())
			.build();
		assertThat(this.observationConvention.supportsContext(observationContext)).isTrue();
		assertThat(this.observationConvention.supportsContext(new Observation.Context())).isFalse();
	}

	@Test
	void shouldHaveRequiredLowCardinalityKeyValues() {
		EmbeddingModelObservationContext observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(generateEmbeddingRequest())
			.operationMetadata(generateOperationMetadata())
			.requestOptions(EmbeddingModelRequestOptions.builder().model("mistral").build())
			.build();
		assertThat(this.observationConvention.getLowCardinalityKeyValues(observationContext)).contains(
				KeyValue.of(LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(), "embedding"),
				KeyValue.of(LowCardinalityKeyNames.AI_PROVIDER.asString(), "ollama"),
				KeyValue.of(LowCardinalityKeyNames.REQUEST_MODEL.asString(), "mistral"));
	}

	@Test
	void shouldHaveOptionalKeyValues() {
		EmbeddingModelObservationContext observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(generateEmbeddingRequest())
			.operationMetadata(generateOperationMetadata())
			.requestOptions(EmbeddingModelRequestOptions.builder()
				.model("supermodel")
				.dimensions(1492)
				.encodingFormat("vector")
				.build())
			.build();
		observationContext.setResponse(new EmbeddingResponse(List.of(),
				new EmbeddingResponseMetadata("mistral-42", new TestUsage(), Map.of())));
		assertThat(this.observationConvention.getLowCardinalityKeyValues(observationContext))
			.contains(KeyValue.of(LowCardinalityKeyNames.RESPONSE_MODEL.asString(), "mistral-42"));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(observationContext)).contains(
				KeyValue.of(HighCardinalityKeyNames.REQUEST_EMBEDDING_DIMENSIONS.asString(), "1492"),
				KeyValue.of(HighCardinalityKeyNames.REQUEST_EMBEDDING_ENCODING_FORMAT.asString(), "vector"),
				KeyValue.of(HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(), "1000"),
				KeyValue.of(HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(), "1000"));
	}

	@Test
	void shouldHaveMissingKeyValues() {
		EmbeddingModelObservationContext observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(generateEmbeddingRequest())
			.operationMetadata(generateOperationMetadata())
			.requestOptions(EmbeddingModelRequestOptions.builder().model("supermodel").build())
			.build();
		assertThat(this.observationConvention.getLowCardinalityKeyValues(observationContext))
			.contains(KeyValue.of(LowCardinalityKeyNames.RESPONSE_MODEL.asString(), KeyValue.NONE_VALUE));
		assertThat(this.observationConvention.getHighCardinalityKeyValues(observationContext)).contains(
				KeyValue.of(HighCardinalityKeyNames.REQUEST_EMBEDDING_DIMENSIONS.asString(), KeyValue.NONE_VALUE),
				KeyValue.of(HighCardinalityKeyNames.REQUEST_EMBEDDING_ENCODING_FORMAT.asString(), KeyValue.NONE_VALUE),
				KeyValue.of(HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(), KeyValue.NONE_VALUE),
				KeyValue.of(HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(), KeyValue.NONE_VALUE));
	}

	private EmbeddingRequest generateEmbeddingRequest() {
		return new EmbeddingRequest(List.of(), EmbeddingOptions.EMPTY);
	}

	private AiOperationMetadata generateOperationMetadata() {
		return AiOperationMetadata.builder()
			.operationType(AiOperationType.EMBEDDING.value())
			.provider(AiProvider.OLLAMA.value())
			.build();
	}

	static class TestUsage implements Usage {

		@Override
		public Long getPromptTokens() {
			return 1000L;
		}

		@Override
		public Long getGenerationTokens() {
			return 0L;
		}

	}

}
