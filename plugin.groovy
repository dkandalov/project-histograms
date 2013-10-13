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

	def persist(String filePath) {
		FileUtil.writeToFile(new File(filePath), JsonOutput.toJson(map))
	}

	def loadFrom(String filePath) {
		def file = new File(filePath)
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

	ProjectHistograms persist(String filePath) {
		allHistograms.each{ it.persist(filePath) }
		this
	}

	ProjectHistograms loadFrom(String filePath) {
		allHistograms.each{ it.loadFrom(filePath) }
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
	def accumulatedHistograms = "accumulated"
	def pathToDataFor = { String name -> "${pluginPath()}/data/${name}-histogram.json" }

	showPopupMenu([
			"Build and Show Histogram": {
				doInBackground("Building histograms"){
					runReadAction{
						def histograms = new ProjectHistograms()
								.process(allPsiItemsIn(project))
								.persist(pathToDataFor(project.name))
						openInBrowser(fillTemplateFrom(histograms, project.name))
					}
				}
			},
			"Build Histogram and Accumulate": {
				doInBackground("Building and accumulating histogram"){
					runReadAction{
						def histograms = new ProjectHistograms()
								.loadFrom(pathToDataFor(accumulatedHistograms))
								.process(allPsiItemsIn(project))
								.persist(pathToDataFor(accumulatedHistograms))
						openInBrowser(fillTemplateFrom(histograms, accumulatedHistograms))
					}
				}
			},
			"Reset Accumulated Data": {
				new File(pathToDataFor(accumulatedHistograms)).delete()
				show("Deleted accumulated data")
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