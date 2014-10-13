package com.keppil.propertytester;

import java.lang.reflect.Method;

/*
 * Immutable class that holds information related to a property field.
 */
public class PropertyInfo {

	private final Method getter;
	private final Method setter;
	private final Class<?> type;

	/**
	 * Standard constructor.
	 *
	 * @param getter
	 *            The get method for this property field.
	 * @param setter
	 *            The set method for this property field.
	 * @param type
	 *            The type of this property field.
	 */
	public PropertyInfo(final Method getter, final Method setter, final Class<?> type) {
		this.getter = getter;
		this.setter = setter;
		this.type = type;
	}

	public Method getGetter() {
		return this.getter;
	}

	public Method getSetter() {
		return this.setter;
	}

	public Class<?> getType() {
		return this.type;
	}
}
