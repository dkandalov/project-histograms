package tests

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiFileFactory
import histograms.PsiStats
import org.junit.Test

class PsiStatsTest {
	@Test void "find amount of methods in class"() {
		if (false) {
			def javaCode = """

	"""
			def fileFactory = PsiFileFactory.getInstance(project)
			def psiFile = fileFactory.createFileFromText("Sample.java", JavaFileType.INSTANCE, javaCode)
			assert PsiStats.allMethodsIn(psiFile).size() == 3
		}

		assert 1 == 2
	}

	@Test void "find amount of fields in class"() {
		assert 2 == 3
	}
}
