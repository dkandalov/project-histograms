package tests

import histograms.Complexity
import org.junit.Test

class ComplexityTest {
	@Test def "calculate complexity"() {
		def complexity = Complexity.of("""
			class Sample {
				Sample() {}
				Sample(int i) {}
			}
		""")
		assert complexity == 0

		complexity = Complexity.of("""
			class Sample {
				Sample() {
					int i = 123;
				}
				Sample(int i) {
					int j = i;
				}
			}
		""")
		assert complexity == 4
	}
}
