package org.burningwave.json;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

@SuppressWarnings("unused")
@RunWith(JUnitPlatform.class)
//@SelectPackages("org.burningwave.json")
@SelectClasses({
	ObjectHandlerTest.class,
	ValidatorTest.class
})
@ExcludeTags("Heavy")
public class AllExceptHeavyTestsSuite {

}