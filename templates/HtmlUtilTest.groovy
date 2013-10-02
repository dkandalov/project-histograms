package templates

import org.junit.Test

class HtmlUtilTest {
	@Test void "should convert map into javascript nested arrays"() {
		assert HtmlUtil.asJsArray([:]) == "[]"
		assert HtmlUtil.asJsArray([1: 2]) == "[[1,2]]"
		assert HtmlUtil.asJsArray([1: 2, 3: 4]) == "[[1,2],[3,4]]"
	}
}
