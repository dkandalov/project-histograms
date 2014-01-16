package tests
import org.junit.Test

import static histograms.Complexity.complexityByLineOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class ComplexityTest {
	@Test void "calculate complexity"() {
		assertThat(asString(complexityByLineOf("""
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

		assertThat(asString(complexityByLineOf("""
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
2          int i = 123;
0        }
0        Sample(int i) {
2          int j = i;
0        }
0      }
"""))
	}

	private static String asString(List complexityByLine) {
		"\n" + complexityByLine.collect{ it[0] + it[1] }.join("\n") + "\n"
	}
}
