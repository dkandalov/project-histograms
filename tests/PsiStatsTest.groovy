package tests
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import histograms.PsiStats
import org.junit.Test

class PsiStatsTest {
	private final Project project

	PsiStatsTest(Project project) {
		this.project = project
	}

	@Test void "find amount of methods in class"() {
		def psiFile = asJavaPsi("Sample.java", """
			class Sample {
				public static void method1() {}
				private int method2() { return 0; }
				class InnerClass {
					public int method3() { return 0; }
				}
			}
		""")
		assert PsiStats.allMethodsIn(psiFile).size() == 3
	}

	@Test void "find amount of fields in class"() {
		def psiFile = asJavaPsi("Sample.java", """
			class Sample {
				public static final int field1;
				private int field2;
				class InnerClass {
					public boolean field3;
				}
			}
		""")
		assert PsiStats.allFieldsIn(psiFile).size() == 3
	}

	private PsiJavaFile asJavaPsi(String fileName, String javaCode) {
		def fileFactory = PsiFileFactory.getInstance(project)
		fileFactory.createFileFromText(fileName, JavaFileType.INSTANCE, javaCode) as PsiJavaFile
	}
}
