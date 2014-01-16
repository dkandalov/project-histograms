package tests
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import histograms.PsiStats
import liveplugin.PluginUtil
import org.junit.Test

class PsiStatsTest {

	@Test void "find amount of methods in class"() {
		def psiFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				Sample(int i) {}
				static void method1() {}
				int method2() { return 0; }
				class InnerClass {
					int method3() { return 0; }
				}
				static class StaticInnerClass {
					int method4() { return 0; }
				}
				Runnable countAnonymousSeparately = new Runnable() {
					@Override public void run() {}
				}
			}
		""")
		assert new PsiStats(psiFile).amountOfMethods == [
				"Sample": 5,
				"Sample.StaticInnerClass": 1,
				"Sample.java-java.lang.Runnable\$0": 1
		]
	}

	@Test void "find amount of fields in class"() {
		def javaFile = asJavaPsi("Sample.java", """
			class Sample {
				public static final int field1;
				private int field2;
				class InnerClass {
					public boolean field3;
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfFields == ["Sample": 3]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				static int onlyField;
				enum IgnoreEnums { A, B, C }
			}
		""")
		assert new PsiStats(javaFile).amountOfFields == ["Sample": 1]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				static int onlyField;
				static class InnerStatic {
					int field1;
					int field2;
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfFields == ["Sample": 1, "Sample.InnerStatic": 2]
	}

	@Test void "find amount of parameters per method"() {
		def javaFile = asJavaPsi("Sample.java", "class Sample {}")
		assert new PsiStats(javaFile).amountOfParametersPerMethod == ["Sample": []]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				Sample(int p1) {}
				void method(int p2, int p3) {}
				class InnerClass {
					void method(int p4, int p5, int p6) {}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfParametersPerMethod == ["Sample": [0, 1, 2, 3]]
	}

	@Test void "find amount of 'if' statements per method"() {
		def javaFile = asJavaPsi("Sample.java", "class Sample {}")
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == ["Sample": []]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				void justOneIf() { if (true) {} else {} }
				void nestedIfs() {
					if (true) {
						if (true) {}
						else {}
					} else {
					}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == ["Sample": [0, 1, 2]]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				void switchStatement(int i) {
					switch (i) {
						case 0: break;
						default: break;
					}
				}
				void nestedSwitchStatements(int i) {
					switch (i) {
						case 0:
							switch (i) {
								case 1: break;
								default: break;
							}
							break;
						default: break;
					}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == ["Sample": [0, 1, 2]]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				int conditionalExpression() { return (true ? 1 : 2); }
				int nestedConditionalExpressions() { return (true ? (true ? 1 : 2) : 3); }
			}
		""")
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == ["Sample": [0, 1, 2]]
	}

	@Test void "find amount of loops per method"() {
		def javaFile = asJavaPsi("Sample.java", "class Sample {}")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == ["Sample": []]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				void forLoop() { for (int i = 0; i < 100; i++) {} }
				void nestedForLoops() {
					for (int i = 0; i < 100; i++) {
						for (int j = 0; j < 100; j++) {}
					}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == ["Sample": [0, 1, 2]]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				void forEachLoop() {
					for (Object o : new ArrayList()) {}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == ["Sample": [1]]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				void whileLoop(int i) {
					while (true) {}
				}
				void nestedWhileLoops(int i) {
					while (true) {
						while(true) {}
					}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == ["Sample": [0, 1, 2]]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				void doWhileLoop() { do {} while (true) }
				void nestedDoWhileLoops() {
					do {
						do {} while (true)
					} while (true)
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == ["Sample": [0, 1, 2]]
	}

	private PsiJavaFile asJavaPsi(String fileName, String javaCode) {
		PluginUtil.runReadAction{
			def fileFactory = PsiFileFactory.getInstance(project)
			fileFactory.createFileFromText(fileName, JavaFileType.INSTANCE, javaCode) as PsiJavaFile
		}
	}

	PsiStatsTest(Map context) {
		this.project = context.project
	}

	private final Project project
}
