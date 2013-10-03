import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod

import static templates.HtmlUtil.asJsArray
import static templates.HtmlUtil.createFromTemplate
import static liveplugin.PluginUtil.*

static List<PsiMethod> allMethodsIn(PsiJavaFile javaFile) {
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

class Histogram {
	final TreeMap map = new TreeMap()

	def add(int value) {
		if (map[value] == null) map[value] = 0
		map[value] = map[value] + 1
	}

	int size() {
		map.size()
	}
}


registerAction("miscProjectHistograms", "ctrl shift H") { event ->
  def project = event.project
  doInBackground("Building histograms") {
    runReadAction {
	    def amountOfMethodsInClasses = new Histogram()
	    def amountOfFieldsInClasses = new Histogram()
	    def amountOfParametersInMethods = new Histogram()

      for (PsiFileSystemItem item : allPsiItemsIn(project)) {
	      if (item == null || !(item instanceof PsiJavaFile)) continue

	      def methods = allMethodsIn(item)
	      def fields = allFieldsIn(item)

	      amountOfFieldsInClasses.add(fields.size())
	      amountOfMethodsInClasses.add(methods.size())
	      methods.each{
		      amountOfParametersInMethods.add(amountOfParametersIn(it))
	      }
      }

	    createFromTemplate("${pluginPath}/templates", "histogram.html", project.name, [
			    "project_name_placeholder" : { project.name },
			    "parameters_per_method_data": { asJsArray(amountOfParametersInMethods.map) },
			    "fields_per_class_data": { asJsArray(amountOfFieldsInClasses.map) },
			    "methods_per_class_data": { asJsArray(amountOfMethodsInClasses.map) },
	    ])
	    show(amountOfParametersInMethods.size())
	    show(amountOfFieldsInClasses.size())
	    show(amountOfMethodsInClasses.size())
    }
  }
}
show("reloaded")