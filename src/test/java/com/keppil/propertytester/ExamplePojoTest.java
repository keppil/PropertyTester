package com.keppil.propertytester;

import java.util.Collections;
import java.util.List;

public class ExamplePojoTest extends PropertyTester<ExamplePojo> {

	public ExamplePojoTest() {
		super(ExamplePojo.class);
		addRandomMethod(List.class, Collections::emptyList);
	}

}
