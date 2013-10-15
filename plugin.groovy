import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import templates.HtmlUtil

import static PsiStatsUtil.*
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

	def toJson() { JsonOutput.toJson(map) }

	def fromJson(String json) {
		def loadedMap = (Map) new JsonSlurper().parseText(json)
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
		FileUtil.writeToFile(new File(filePath), allHistograms.collect{ it.toJson() }.join("\n"))
		this
	}

	ProjectHistograms loadFrom(String filePath) {
		def file = new File(filePath)
		if (!file.exists()) return this

		FileUtil.loadFile(file).split(/\n/).eachWithIndex{ String line, int i ->
			allHistograms[i].fromJson(line)
		}
		this
	}
}

class PsiStatsUtil {

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

	static int amountOfIfStatementsIn(PsiMethod method) {
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


registerAction("miscProjectHistograms", "ctrl shift H") { AnActionEvent event ->
  def project = event.project
	def accumulatedHistograms = "accumulated"
	def pathToDataFor = { String name -> "${pluginPath()}/data/${name}-histogram.json" }

	showPopupMenu([
//			"amountOfIfStatementsIn": {
//				currentPsiFileIn(project).accept(new JavaRecursiveElementVisitor() {
//					@Override void visitMethod(PsiMethod method) {
//						show(method.name + " : " + amountOfIfStatementsIn(method))
//					}
//				})
//			},
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
						new ProjectHistograms()
								.loadFrom(pathToDataFor(accumulatedHistograms))
								.process(allPsiItemsIn(project))
								.persist(pathToDataFor(accumulatedHistograms))
						show("Accumulated histograms from ${project.name}")
					}
				}
			},
			"Reset Accumulated Data": {
				new File(pathToDataFor(accumulatedHistograms)).delete()
				show("Deleted accumulated data")
			},
			"Show Accumulated Histogram": {
				def histograms = new ProjectHistograms()
						.loadFrom(pathToDataFor(accumulatedHistograms))
				openInBrowser(fillTemplateFrom(histograms, accumulatedHistograms))
			}
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