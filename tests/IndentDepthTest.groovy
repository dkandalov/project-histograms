package tests
import org.junit.Test

import static histograms.WiltComplexity.indentDepthByLineOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class IndentDepthTest {
	@Test void "calculate indent depth for simple cases"() {
		assertThat(asString(indentDepthByLineOf("""
      class Sample {
        Sample() {}
        Sample(int i) {}
      }
    """)), equalTo("""
0      class Sample {
0        Sample() {}
0        Sample(int i) {}
0      }
"""))

		assertThat(asString(indentDepthByLineOf("""
      class Sample {
        Sample() {
          int i = 123;
        }
        Sample(int i) {
          int j = i;
        }
      }
    """)), equalTo("""
0      class Sample {
0        Sample() {
1          int i = 123;
0        }
0        Sample(int i) {
1          int j = i;
0        }
0      }
"""))
	}

	private static String asString(List indentDepthByLine) {
		"\n" + indentDepthByLine.collect{ it[0] + it[1] }.join("\n") + "\n"
	}
}
