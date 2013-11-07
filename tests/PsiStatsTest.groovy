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
				Sample() {}
				Sample(int i) {}
				static void method1() {}
				int method2() { return 0; }
				class InnerClass {
					int method3() { return 0; }
				}
			}
		""")
		assert new PsiStats(psiFile).amountOfMethods == 5
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
		assert new PsiStats(javaFile).amountOfParametersPerMethod == []

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
		assert new PsiStats(javaFile).amountOfParametersPerMethod == [0, 1, 2, 3]
	}

	@Test void "find amount of 'if' statements per method"() {
		def javaFile = asJavaPsi("Sample.java", "class Sample {}")
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == []

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
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == [0, 1, 2]

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
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == [0, 1, 2]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				Sample() {}
				int conditionalExpression() { return (true ? 1 : 2); }
				int nestedConditionalExpressions() { return (true ? (true ? 1 : 2) : 3); }
			}
		""")
		assert new PsiStats(javaFile).amountOfIfStatementsPerMethod == [0, 1, 2]
	}

	@Test void "find amount of loops per method"() {
		def javaFile = asJavaPsi("Sample.java", "class Sample {}")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == []

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
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == [0, 1, 2]

		javaFile = asJavaPsi("Sample.java", """
			class Sample {
				void forEachLoop() {
					for (Object o : new ArrayList()) {}
				}
			}
		""")
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == [1]

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
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == [0, 1, 2]

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
		assert new PsiStats(javaFile).amountOfLoopsPerMethod == [0, 1, 2]
	}

	private PsiJavaFile asJavaPsi(String fileName, String javaCode) {
		def fileFactory = PsiFileFactory.getInstance(project)
		fileFactory.createFileFromText(fileName, JavaFileType.INSTANCE, javaCode) as PsiJavaFile
	}
}
