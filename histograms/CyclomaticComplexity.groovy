package histograms

import com.intellij.psi.PsiJavaFile
import com.siyeh.ig.methodmetrics.CyclomaticComplexityVisitor

@SuppressWarnings("GroovyAccessibility")
class CyclomaticComplexity {
	static int calculateFor(PsiJavaFile javaFile) {
		def complexityVisitor = new CyclomaticComplexityVisitor()
		complexityVisitor.visitFile(javaFile)
		complexityVisitor.complexity
	}
}
