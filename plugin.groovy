import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod

import static liveplugin.PluginUtil.*

static List<PsiMethod> allMethodsIn(PsiJavaFile javaFile) {
  def result = []
  javaFile.acceptChildren(new JavaRecursiveElementVisitor() {
    @Override void visitMethod(PsiMethod method) { result << method }
  })
  result
}

static List<PsiField> allFieldsIn(PsiJavaFile javaFile) {
	def result = []
	javaFile.acceptChildren(new JavaRecursiveElementVisitor() {
		@Override void visitField(PsiField field) { result << field }
	})
	result
}

static int amountOfParametersIn(PsiMethod method) {
  method.parameterList.parametersCount
}

static TreeMap makeHistogram(List values) {
  new TreeMap(values.groupBy{it}.collectEntries{[it.key, it.value.size()]})
}

class Histogram {
	final TreeMap map = new TreeMap()

	def add(int value) {
		if (map[value] == null) map[value] = 0
		map[value] = map[value] + 1
	}
}

registerAction("miscProjectHistograms", "ctrl shift H") { event ->
  def project = event.project
  doInBackground("Building histograms") {
    runReadAction {
	    def amountOfMethodsInClass = new Histogram()
	    def amountOfFieldsInClass = new Histogram()
	    def amountOfParametersInMethod = new Histogram()

      for (PsiFileSystemItem item : allPsiItemsIn(project)) {
	      if (item == null || !(item instanceof PsiJavaFile)) continue

	      def methods = allMethodsIn(item)
	      def fields = allFieldsIn(item)

	      amountOfFieldsInClass.add(fields.size())
	      amountOfMethodsInClass.add(methods.size())
	      methods.each{
		      amountOfParametersInMethod.add(amountOfParametersIn(it))
	      }
      }

      show(amountOfFieldsInClass.map)
      show(amountOfMethodsInClass.map)
      show(amountOfParametersInMethod.map)
    }
  }
}

