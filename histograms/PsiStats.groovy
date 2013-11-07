package histograms
import com.intellij.psi.*

import static com.intellij.psi.PsiModifier.STATIC

class PsiStats {
	private final List<PsiClass> classes
	private final Map<PsiClass, List<PsiMethod>> methods
	private final Map<PsiClass, List<PsiField>> fields

	PsiStats(PsiJavaFile javaFile) {
		classes = allClassesIn(javaFile)
		methods = classes.collectEntries{ [it, allMethodsIn(it)] }
		fields = classes.collectEntries{ [it, allFieldsIn(it)] }
	}

	Map<String, Integer> getAmountOfFields() {
		fields.collectEntries{ [it.key.qualifiedName, it.value.size()] } as Map<String, Integer>
	}
	Map<String, Integer> getAmountOfMethods() {
		methods.collectEntries{ [it.key.qualifiedName, it.value.size()] } as Map<String, Integer>
	}

	Map<String, Collection<Integer>> getAmountOfParametersPerMethod() {
		methods.collectEntries{ psiClass, psiMethods -> [
				psiClass.qualifiedName,
				psiMethods.collect{ amountOfParametersIn(it) }
		]} as Map<String, Collection<Integer>>
	}

	Map<String, Collection<Integer>> getAmountOfIfStatementsPerMethod() {
		methods.collectEntries{ psiClass, psiMethods -> [
				psiClass.qualifiedName,
				psiMethods.collect{ amountOfIfStatementsIn(it) }
		]} as Map<String, Collection<Integer>>
	}

	Map<String, Collection<Integer>> getAmountOfLoopsPerMethod() {
		methods.collectEntries{ psiClass, psiMethods -> [
				psiClass.qualifiedName,
				psiMethods.collect{ amountOfLoopsIn(it) }
		]} as Map<String, Collection<Integer>>
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

	private static List<PsiMethod> allMethodsIn(PsiClass psiClass) {
		def result = []
		def visit = null
		visit = { PsiElement psiElement ->
			psiElement.acceptChildren(new JavaElementVisitor() {
				@Override void visitElement(PsiElement element) {
					if (element instanceof PsiClass && element.enum) null
					else if (element instanceof PsiClass && element.modifierList.hasModifierProperty(STATIC)) null
					else if (element instanceof PsiMethod) result << element
					else visit(element)
				}
			})
		}
		visit(psiClass)
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
