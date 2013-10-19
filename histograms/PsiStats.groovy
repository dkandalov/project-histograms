package histograms

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSwitchLabelStatement

class PsiStats {
	private final List<PsiMethod> methods
	private final List<PsiField> fields

	PsiStats(PsiJavaFile javaFile) {
		methods = allMethodsIn(javaFile)
		fields = allFieldsIn(javaFile)
	}

	int getAmountOfFields() { fields.size() }
	int getAmountOfMethods() { methods.size() }

	Collection<Integer> getAmountOfParametersPerMethod() {
		methods.collect{ amountOfParametersIn(it) }
	}

	Collection<Integer> getAmountOfIfStatementsPerMethod() {
		methods.collect{ amountOfIfStatementsIn(it) }
	}

	private static List<PsiMethod> allMethodsIn(PsiJavaFile javaFile) {
		def result = []
		javaFile.acceptChildren(new JavaRecursiveElementVisitor() {
			@Override void visitMethod(PsiMethod method) {
				// ignore interfaces assuming that everything will be counted in their implementations
				// or if there are not implementations, then it probably can be ignored
				if (method.containingClass.interface) return

				result << method
			}
		})
		result
	}

	private static List<PsiField> allFieldsIn(PsiJavaFile javaFile) {
		def result = []
		javaFile.acceptChildren(new JavaRecursiveElementVisitor() {
			@Override void visitField(PsiField field) { result << field }
		})
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
				}
			})
		}
		visit(method)
		counter
	}
}
