import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import templates.HtmlUtil

import static liveplugin.PluginUtil.*
import static templates.HtmlUtil.createFromTemplate

String pluginPath() { pluginPath }

static openInBrowser(File file) { BrowserUtil.open("file://${file.absolutePath}") }

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

	def loadFrom(String path, String name) {
		def file = new File(path + "/data/${name}-histogram.json")
		if (!file.exists()) return

		def loadedMap = (Map) new JsonSlurper().parseText(FileUtil.loadFile(file))
				.collectEntries{ [Integer.parseInt(it.key), it.value] }

		map.clear()
		map.putAll(loadedMap)
	}

	String asJsArray() { HtmlUtil.asJsArray(map) }
}

class ProjectHistograms {
	final def amountOfMethodsInClasses = new Histogram()
	final def amountOfFieldsInClasses = new Histogram()
	final def amountOfParametersInMethods = new Histogram()
	final def allHistograms = [amountOfMethodsInClasses, amountOfFieldsInClasses, amountOfParametersInMethods]


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
		allHistograms.each{ it.persist(path, name) }
		this
	}

	ProjectHistograms loadFrom(String path, String name) {
		allHistograms.each{ it.loadFrom(path, name) }
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
			"Build and Show Histogram": {
				doInBackground("Building histograms"){
					runReadAction{
						def histograms = new ProjectHistograms()
								.process(allPsiItemsIn(project))
								.persist(pluginPath(), project.name)
						openInBrowser(fillTemplateFrom(histograms, project.name))
					}
				}
			},
			"Build Histogram and Accumulate": {
				doInBackground("Building and accumulating histogram"){
					runReadAction{
						def name = "accumulated"
						def histograms = new ProjectHistograms()
								.loadFrom(pluginPath(), name)
								.process(allPsiItemsIn(project))
								.persist(pluginPath(), name)
						openInBrowser(fillTemplateFrom(histograms, name))
					}
				}
			},
			"Reset Accumulator": {

			},
			"Show Accumulated Histogram": {  }, // TODO
	], "Histograms")
}


File fillTemplateFrom(ProjectHistograms histograms, String name) {
	createFromTemplate("${pluginPath()}/templates", "histogram.html", name, [
			"project_name_placeholder": { name },
			"parameters_per_method_data": { histograms.amountOfParametersInMethods.asJsArray() },
			"fields_per_class_data": { histograms.amountOfFieldsInClasses.asJsArray() },
			"methods_per_class_data": { histograms.amountOfMethodsInClasses.asJsArray() },
	])
}

show("reloaded")