package org.burningwave.json;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

@SuppressWarnings("unused")
@RunWith(JUnitPlatform.class)
//@SelectPackages("org.burningwave.json")
@SelectClasses({

})
@ExcludeTags("Heavy")
public class AllExceptHeavyTestsSuite {

}