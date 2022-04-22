package com.navercorp.fixturemonkey.introspector;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.navercorp.fixturemonkey.api.generator.ArbitraryGeneratorContext;
import com.navercorp.fixturemonkey.api.generator.ArbitraryProperty;
import com.navercorp.fixturemonkey.api.generator.ArbitraryPropertyGenerator;
import com.navercorp.fixturemonkey.api.generator.ArbitraryPropertyGeneratorContext;
import com.navercorp.fixturemonkey.api.generator.DefaultNullInjectGenerator;
import com.navercorp.fixturemonkey.api.introspector.ArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.ArbitraryIntrospectorResult;
import com.navercorp.fixturemonkey.api.introspector.BooleanIntrospector;
import com.navercorp.fixturemonkey.api.introspector.CompositeArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.EnumIntrospector;
import com.navercorp.fixturemonkey.api.introspector.JavaArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.JavaTimeArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.UuidIntrospector;
import com.navercorp.fixturemonkey.api.matcher.MatcherOperator;
import com.navercorp.fixturemonkey.api.option.GenerateOptions;
import com.navercorp.fixturemonkey.api.property.PropertyCache;
import com.navercorp.fixturemonkey.api.property.RootProperty;
import com.navercorp.fixturemonkey.api.type.TypeReference;
import com.navercorp.fixturemonkey.jackson.introspector.JacksonArbitraryIntrospector;
import com.navercorp.fixturemonkey.jackson.property.JacksonPropertyNameResolver;

class JacksonArbitraryIntrospectorTest {
	private final ArbitraryIntrospector arbitraryIntrospector = new CompositeArbitraryIntrospector(
		Arrays.asList(
			new EnumIntrospector(),
			new BooleanIntrospector(),
			new UuidIntrospector(),
			new JavaArbitraryIntrospector(),
			new JavaTimeArbitraryIntrospector()
		)
	);

	@Test
	void introspect() {
		// given
		JacksonArbitraryIntrospector sut = JacksonArbitraryIntrospector.INSTANCE;

		TypeReference<JacksonSample> typeReference = new TypeReference<JacksonSample>() {
		};
		RootProperty rootProperty = new RootProperty(typeReference.getAnnotatedType());

		GenerateOptions generateOptions = new GenerateOptions(
			GenerateOptions.DEFAULT_ARBITRARY_PROPERTY_GENERATORS,
			Collections.singletonList(new MatcherOperator<>(p -> true, new JacksonPropertyNameResolver())),
			Collections.emptyList(),
			new DefaultNullInjectGenerator(),
			Collections.emptyList(),
			3,
			Collections.emptyList(),
			GenerateOptions.DEFAULT_ARBITRARY_GENERATOR
		);

		ArbitraryPropertyGenerator rootGenerator = generateOptions.getArbitraryPropertyGenerator(rootProperty);
		ArbitraryProperty rootArbitraryProperty = rootGenerator.generate(
			new ArbitraryPropertyGeneratorContext(
				rootProperty,
				null,
				null,
				generateOptions
			)
		);

		List<ArbitraryProperty> childrenProperties = PropertyCache.getProperties(typeReference.getAnnotatedType())
			.stream()
			.map(it -> generateOptions.getArbitraryPropertyGenerator(it)
				.generate(
					new ArbitraryPropertyGeneratorContext(
						it,
						null,
						rootArbitraryProperty,
						generateOptions
					)
				)
			)
			.collect(toList());

		ArbitraryGeneratorContext context = new ArbitraryGeneratorContext(
			rootArbitraryProperty,
			childrenProperties,
			null,
			(ctx, prop) -> this.arbitraryIntrospector.introspect(
				new ArbitraryGeneratorContext(
					prop,
					Collections.emptyList(),
					ctx,
					(ctx2, prop2) -> null
				)
			).getValue()
		);

		// when
		ArbitraryIntrospectorResult actual = sut.introspect(context);

		then(actual.getValue()).isNotNull();

		JacksonSample sample = (JacksonSample)actual.getValue().sample();
		then(sample.getName()).isNotNull();
		then(sample.getValue()).isNotNull();
		then(sample.getSeason()).isNotNull();
	}

	static class JacksonSample {
		private String name;

		@JsonProperty("x_value")
		private int value;

		private Season season;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getValue() {
			return this.value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public Season getSeason() {
			return this.season;
		}

		public void setSeason(Season season) {
			this.season = season;
		}
	}

	enum Season {
		SPRING,
		SUMMER,
		FALL,
		WINTER
	}
}