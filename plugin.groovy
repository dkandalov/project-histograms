import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod

import java.util.regex.Matcher

import static HttpUtil.asJsString
import static HttpUtil.createFromTemplate
import static liveplugin.PluginUtil.*

static List<PsiMethod> allMethodsIn(PsiJavaFile javaFile) {
	// TODO exclude interfaces?
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
	// TODO exclude interfaces?
  method.parameterList.parametersCount
}

static TreeMap makeHistogram(List values) {
  new TreeMap(values.groupBy{it}.collectEntries{[it.key, it.value.size()]})
}

class HttpUtil {
	static void createFromTemplate(String templateFolder, String template, String projectName, String jsValue) {
		def templateText = new File("$templateFolder/$template").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("$templateFolder/$fileName").readLines().join("\n") }
		text = fillDataPlaceholder(text, jsValue)
		text = fillProjectNamePlaceholder(text, "\"$projectName\"")
		new File("${templateFolder}/${projectName}_${template}").write(text)
	}

	static String inlineJSLibraries(String html, Closure<String> sourceCodeReader) {
		(html =~ /(?sm).*?<script src="(.*?)"><\/script>.*/).with{
			if (!matches()) html
			else inlineJSLibraries(
					html.replace("<script src=\"${group(1)}\"></script>", "<script>${sourceCodeReader(group(1))}</script>"),
					sourceCodeReader
			)
		}
	}

	static String fillProjectNamePlaceholder(String templateText, String projectName) {
		templateText.replaceFirst(/(?s)\/\*project_name_placeholder\*\/.*\/\*project_name_placeholder\*\//, Matcher.quoteReplacement(projectName))
	}

	static String fillDataPlaceholder(String templateText, String jsValue) {
		templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
	}

	static String asJsString(List list) {
		String newLine = "\\n\\\n"
		def result = "\""
		result += "metric" + newLine
		result += list.join(newLine)
		result + newLine + "\""
	}
}

registerAction("miscProjectHistograms", "ctrl shift H") { event ->
  def project = event.project
  doInBackground("Building histograms") {
    runReadAction {
	    def amountOfMethodsInClass = []
	    def amountOfFieldsInClass = []
	    def amountOfParametersInMethod = []

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

	    createFromTemplate("${pluginPath}/templates", "histogram.html", project.name, asJsString(amountOfParametersInMethod))
      show(amountOfFieldsInClass.size())
      show(amountOfMethodsInClass.size())
      show(amountOfParametersInMethod.size())
    }
  }
}
show("reloaded")