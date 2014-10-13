package com.keppil.propertytester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.junit.Test;

/**
 * A class intended to be used as a base class to provide automatic testing of all properties of a class. All fields are assumed to be standard
 * properties with a public getter and setter.
 * <p>
 *
 * Sample usage:
 *
 * <pre>
 * public class ExamplePojoTest extends PropertyTester&lt;ExamplePojo&gt; {
 * 	public ExamplePojoTest() {
 * 		super(ExamplePojo.class);
 * 	}
 * 	// More tests...
 * }
 * </pre>
 *
 * @param <T>
 *            The type of the class to be tested.
 */
public class PropertyTester<T> {

	private static final int REPETITIONS_FOR_RANDOM_TEST = 10;
	private static final List<String> getterPrefixes = ListUtils.unmodifiableList(Arrays.asList("get", "is"));
	private static final String setterPrefix = "set";

	private final Class<T> clazz;
	private static Map<Class<?>, Supplier<Object>> standardRandomMethods;

	private final Map<Class<?>, Supplier<Object>> customRandomMethods = new HashMap<>();
	private final Set<String> excludedProperties = new HashSet<>();

	{
		final Map<Class<?>, Supplier<Object>> standardMethods = new HashMap<>();
		final Random random = new Random();
		standardMethods.put(byte.class, () -> (byte) random.nextInt());
		standardMethods.put(Byte.class, () -> (byte) random.nextInt());
		standardMethods.put(short.class, () -> (short) random.nextInt());
		standardMethods.put(Short.class, () -> (short) random.nextInt());
		standardMethods.put(int.class, random::nextInt);
		standardMethods.put(Integer.class, random::nextInt);
		standardMethods.put(long.class, random::nextLong);
		standardMethods.put(Long.class, random::nextLong);
		standardMethods.put(float.class, () -> (float) random.nextGaussian());
		standardMethods.put(Float.class, () -> (float) random.nextGaussian());
		standardMethods.put(double.class, random::nextGaussian);
		standardMethods.put(Double.class, random::nextGaussian);
		standardMethods.put(boolean.class, random::nextBoolean);
		standardMethods.put(Boolean.class, random::nextBoolean);
		standardMethods.put(char.class, () -> (char) random.nextInt());
		standardMethods.put(Character.class, () -> (char) random.nextInt());
		standardMethods.put(String.class, () -> Long.toHexString(random.nextLong()));
		standardMethods.put(List.class, Collections::emptyList);
		standardMethods.put(Set.class, Collections::emptySet);
		standardMethods.put(Map.class, Collections::emptyMap);
		standardRandomMethods = Collections.unmodifiableMap(standardMethods);

		// Needed when using EclEmma plugin
		excludedProperties.add("$jacocoData");
	}

	/**
	 * Standard constructor.
	 *
	 * @param clazz
	 *            The type of the class to be tested.
	 */
	public PropertyTester(final Class<T> clazz) {
		this.clazz = clazz;
	}

	/**
	 * This method can be used to add a random generating function for a property of a type that is not supported out of the box by
	 * {@link PropertyTester}.
	 * <p>
	 * Sample usage:
	 *
	 * <pre>
	 *
	 * public ExamplePojoTest() {
	 * 	super(ExamplePojo.class);
	 * 	addRandomMethod(MyPropertyType.class, () -&gt; new MyPropertyType(new Random().nextInt()));
	 * }
	 * </pre>
	 *
	 * @param clazz
	 *            The type of the property.
	 * @param randomMethod
	 *            The generating method to be used.
	 */
	public void addRandomMethod(final Class<?> clazz, final Supplier<Object> randomMethod) {
		this.customRandomMethods.put(clazz, randomMethod);
	}

	/**
	 * This method can be used to exclude one or more properties from testing. This might be useful if they don't conform to the standard contract for
	 * a property.
	 * <p>
	 * Sample usage:
	 *
	 * <pre>
	 *
	 * public ExamplePojoTest() {
	 * 	super(ExamplePojo.class);
	 * 	excludeProperties(&quot;myNonStandardProperty&quot;);
	 * }
	 * </pre>
	 *
	 * @param propertyNames
	 *            The names of the property/properties to exclude.
	 */
	public void excludeProperties(final String... propertyNames) {
		this.excludedProperties.addAll(Arrays.asList(propertyNames));
	}

	/**
	 * A test that makes sure that all properties with a non-primitive type can be set to {@code null} and be retrieved.
	 *
	 */
	@Test
	public void nullValuesAreHandledCorrectly() throws Exception {
		Set<PropertyInfo> propertiesToTest = findPropertiesToTest(field -> !field.getType().isPrimitive());
		testValues(propertiesToTest, any -> null, 1);
	}

	/**
	 * A test that makes sure that all properties can be set to a random value, which can then be retrieved. This is repeated 10 times for each
	 * property.
	 *
	 */
	@Test
	public void randomValuesAreHandledCorrectly() throws Exception {
		Set<PropertyInfo> propertiesToTest = findPropertiesToTest(any -> true);
		testValues(propertiesToTest, this::randomInstanceOf, REPETITIONS_FOR_RANDOM_TEST);
	}

	private Set<PropertyInfo> findPropertiesToTest(Predicate<Field> fieldFilter) {
		final Set<PropertyInfo> properties = new HashSet<>();
		final List<Field> filteredFields = Stream.of(this.clazz.getDeclaredFields()).filter(fieldFilter)
				.filter(field -> !this.excludedProperties.contains(field.getName())).collect(Collectors.toList());
		final Method[] allMethods = this.clazz.getDeclaredMethods();
		for (final Field field : filteredFields) {
			properties.add(findMethodsForField(field, allMethods));
		}
		return properties;
	}

	private void testValues(final Collection<PropertyInfo> methodHoldersToTest, final Function<Class<?>, Object> argumentFunction,
			final int repetitions) throws Exception {
		final T instance = this.clazz.getConstructor().newInstance();
		for (int i = 0; i < repetitions; i++) {
			for (final PropertyInfo methodHolder : methodHoldersToTest) {
				final Object argument = argumentFunction.apply(methodHolder.getType());
				try {
					methodHolder.getSetter().invoke(instance, new Object[] { argument });
				} catch (final IllegalArgumentException e) {
					fail(String.format("Randomly generated argument for method %s is of type %s instead of the expected type %s.", methodHolder
							.getSetter().getName(), argument.getClass(), methodHolder.getType()));
				}
				final Object result = methodHolder.getGetter().invoke(instance, ArrayUtils.EMPTY_OBJECT_ARRAY);
				assertEquals(String.format("%s did not return the passed value %s!", methodHolder.getGetter().getName(), argument), argument, result);
			}
		}
	}

	private Object randomInstanceOf(final Class<?> clazz) {
		Object randomInstance = null;
		if (standardRandomMethods.containsKey(clazz)) {
			randomInstance = standardRandomMethods.get(clazz).get();
		} else if (this.customRandomMethods.containsKey(clazz)) {
			randomInstance = this.customRandomMethods.get(clazz).get();
		} else {
			try {
				randomInstance = clazz.getConstructor().newInstance();
			} catch (final Exception e) {
				fail(String.format("Unable to create a random instance of '%s'. Either add a no argument constructor to the %1$s class or provide a way to generate an instance via PropertyTester#addRandomMethod().",
							clazz.getSimpleName()));
			}
		}
		return randomInstance;
	}

	private PropertyInfo findMethodsForField(final Field field, final Method[] allMethods) {
		final String name = field.getName();
		return new PropertyInfo(findGetter(name, allMethods), findSetter(name, allMethods), field.getType());
	}

	private Method findGetter(final String propertyName, final Method[] allMethods) {
		for (final String getterPrefix : getterPrefixes) {
			final String getterName = getterPrefix.concat(WordUtils.capitalize(propertyName));
			for (final Method method : allMethods) {
				if (method.getName().equals(getterName)) {
					return method;
				}
			}
		}
		fail(String.format("No getter with any of the prefixes %s could be found for property '%s'.", getterPrefixes, propertyName));
		return null;
	}

	private Method findSetter(final String propertyName, final Method[] allMethods) {
		final String setterName = setterPrefix.concat(WordUtils.capitalize(propertyName));
		for (final Method method : allMethods) {
			if (method.getName().equals(setterName)) {
				return method;
			}
		}
		fail(String.format("No setter named '%s()' could be found for property '%s'.", setterName, propertyName));
		return null;
	}
}
