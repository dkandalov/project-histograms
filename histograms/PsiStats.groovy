package histograms
import com.intellij.psi.*

import static com.intellij.psi.PsiModifier.STATIC

class PsiStats {
	private final List<PsiMethod> methods
	private final Map<PsiClass, PsiField> fields

	PsiStats(PsiJavaFile javaFile) {
		methods = allMethodsIn(javaFile)
		fields = allClassesIn(javaFile).collectEntries{ [it, allFieldsIn(it)] }
	}

	Map<String, Integer> getAmountOfFields() {
		fields.collectEntries{ [it.key.qualifiedName, it.value.size()] } as Map<String, Integer>
	}
	int getAmountOfMethods() { methods.size() }

	Collection<Integer> getAmountOfParametersPerMethod() {
		methods.collect{ amountOfParametersIn(it) }
	}

	Collection<Integer> getAmountOfIfStatementsPerMethod() {
		methods.collect{ amountOfIfStatementsIn(it) }
	}

	Collection<Integer> getAmountOfLoopsPerMethod() {
		methods.collect{ amountOfLoopsIn(it) }
	}

	private static List<PsiClass> allClassesIn(PsiJavaFile javaFile) {
		def result = []
		javaFile.acceptChildren(new JavaRecursiveElementVisitor() {
			@Override void visitClass(PsiClass psiClass) {
				// ignore interfaces assuming that everything will be counted in their implementations
				// or if there are no implementations, then it probably should be ignored
				if (psiClass.interface) return
				if (psiClass.enum) return
				if (psiClass.scope != javaFile && !psiClass.modifierList.hasModifierProperty(STATIC)) return

				result << psiClass

				super.visitElement(psiClass)
			}
		})
		result
	}

	private static List<PsiMethod> allMethodsIn(PsiJavaFile javaFile) {
		def result = []
		javaFile.acceptChildren(new JavaRecursiveElementVisitor() {
			@Override void visitMethod(PsiMethod method) {
				// ignore interfaces assuming that everything will be counted in their implementations
				// or if there are no implementations, then it probably should be ignored
				if (method.containingClass.interface) return

				result << method
			}
		})
		result
	}

	private static List<PsiField> allFieldsIn(PsiClass psiClass) {
		def result = []
		def visit = null
		visit = { PsiElement psiElement ->
			psiElement.acceptChildren(new JavaElementVisitor() {
				@Override void visitElement(PsiElement element) {
					if (element instanceof PsiClass && element.enum) null
					else if (element instanceof PsiClass && element.modifierList.hasModifierProperty(STATIC)) null
					else if (element instanceof PsiField) result << element
					else visit(element)
				}
			})
		}
		visit(psiClass)
		result
	}

	private static int amountOfParametersIn(PsiMethod method) {
		method.parameterList.parametersCount
	}

	private static int amountOfIfStatementsIn(PsiMethod method) {
		int counter = 0
		def visit = null
		visit = { PsiElement element ->
			element.acceptChildren(new JavaRecursiveElementVisitor() {
				@Override void visitIfStatement(PsiIfStatement ifStatement) {
					counter++
					visit(ifStatement)
				}

				@Override void visitSwitchLabelStatement(PsiSwitchLabelStatement statement) {
					if (!statement.defaultCase) counter++
				}

				@Override void visitConditionalExpression(PsiConditionalExpression expression) {
					counter++
					visit(expression)
				}
			})
		}
		visit(method)
		counter
	}

	private static int amountOfLoopsIn(PsiMethod method) {
		int counter = 0
		def visit = null
		visit = { PsiElement element ->
			element.acceptChildren(new JavaRecursiveElementVisitor() {
				@Override void visitForStatement(PsiForStatement statement) {
					counter++
					visit(statement)
				}

				@Override void visitForeachStatement(PsiForeachStatement statement) {
					counter++
					visit(statement)
				}

				@Override void visitWhileStatement(PsiWhileStatement statement) {
					counter++
					visit(statement)
				}

				@Override void visitDoWhileStatement(PsiDoWhileStatement statement) {
					counter++
					visit(statement)
				}
			})
		}
		visit(method)
		counter
	}
}
