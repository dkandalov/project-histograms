import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import groovy.json.JsonOutput

import static liveplugin.PluginUtil.*
import static templates.HtmlUtil.asJsArray
import static templates.HtmlUtil.createFromTemplate

String pluginPath() { pluginPath }

class Histogram {
	final TreeMap map = new TreeMap()

	def add(int value) {
		if (map[value] == null) map[value] = 0
		map[value] = map[value] + 1
	}

	int size() {
		map.size()
	}

	def persist(String path, String name) {
		FileUtil.writeToFile(new File(path + "/data/${name}-histogram.json"), JsonOutput.toJson(map))
	}
}

class ProjectHistograms {
	def amountOfMethodsInClasses = new Histogram()
	def amountOfFieldsInClasses = new Histogram()
	def amountOfParametersInMethods = new Histogram()

	ProjectHistograms process(Iterator<PsiFileSystemItem> items) {
		for (PsiFileSystemItem item : items) {
			if (item == null || !(item instanceof PsiJavaFile)) continue

			def methods = allMethodsIn(item)
			def fields = allFieldsIn(item)

			amountOfFieldsInClasses.add(fields.size())
			amountOfMethodsInClasses.add(methods.size())
			methods.each{
				amountOfParametersInMethods.add(amountOfParametersIn(it))
			}
		}
		this
	}

	ProjectHistograms persist(String path, String name) {
		amountOfFieldsInClasses.persist(path, name)
		amountOfMethodsInClasses.persist(path, name)
		amountOfParametersInMethods.persist(path, name)
		this
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
}


registerAction("miscProjectHistograms", "ctrl shift H") { AnActionEvent event ->
  def project = event.project

	showPopupMenu([
			"Build and Show Histogram": { buildHistogramFor(project) },
			"Build Histogram and Accumulate": {  }, // TODO
			"Reset Accumulator": {  }, // TODO
			"Show Accumulated Histogram": {  }, // TODO
	], "Histograms")
}

def buildHistogramFor(Project project) {
	doInBackground("Building histograms") {
		runReadAction {
			def histograms = new ProjectHistograms()
					.process(allPsiItemsIn(project))
					.persist(pluginPath(), project.name)
			openInBrowser(fillTemplateFrom(histograms))
		}
	}
}

File fillTemplateFrom(ProjectHistograms histograms) {
	createFromTemplate("${pluginPath()}/templates", "histogram.html", project.name, [
			"project_name_placeholder": { project.name },
			"parameters_per_method_data": { asJsArray(histograms.amountOfParametersInMethods.map) },
			"fields_per_class_data": { asJsArray(histograms.amountOfFieldsInClasses.map) },
			"methods_per_class_data": { asJsArray(histograms.amountOfMethodsInClasses.map) },
	])
}

static openInBrowser(File file) { BrowserUtil.open("file://${file.absolutePath}") }

show("reloaded")