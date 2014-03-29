import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import histograms.ProjectHistograms

import static com.intellij.openapi.util.text.StringUtil.capitalize
import static liveplugin.PluginUtil.*
import static templates.HtmlUtil.createFromTemplate

if (false) return createGitHubPages()
if (false) return accumulate()

registerAction("miscProjectHistograms", "ctrl shift H", TOOLS_MENU, "Project Histograms") { AnActionEvent event ->
  def project = event.project

	doInBackground("Building histograms"){
		def histograms = new ProjectHistograms()

		def psiItems = allPsiItemsIn(project)
		while (psiItems.hasNext()) {
			runReadAction{
				def item = psiItems.next()
//				log("histograms: " + item)
				histograms.process(item)
			}
		}
		histograms.persistHistogramsTo(pathToDataFor(project.name))

//		showInConsole(maxItemsAsString(histograms.maxValueItems), project)
		new File("${pluginPath()}/data/${project.name}-top.txt").withWriter {
			it.write(maxItemsAsString(histograms.maxValueItems))
		}
		openInBrowser(fillTemplateWith(histograms, project.name))
	}
}
if (!isIdeStartup) show("reloaded project histograms plugin")


String maxItemsAsString(Map maxItems) {
	maxItems.entrySet().collect{ entry ->
		def name = entry.key
		def itemsByValue = entry.value
		name + "\n" + itemsByValue.collect{"" + it.key + " - " + it.value}.join("\n")
	}.join("\n\n")
}

String pluginPath() { pluginPath }

String pathToDataFor(String name) { "${pluginPath()}/data/${name}-histogram.json" }

static openInBrowser(File file) { BrowserUtil.open("file://${file.absolutePath}") }

File fillTemplateWith(ProjectHistograms histograms, String name,
                      String templateFolder = "${pluginPath()}/templates", String template = "histogram.html") {
	createFromTemplate(templateFolder, template, name, [
			"project_name_placeholder": { capitalize(name) },
			"parameters_per_method_data": { histograms.amountOfParametersInMethods.asJsArray() },
			"ifs_per_method_data": { histograms.amountOfIfsInMethods.asJsArray() },
			"loops_per_method_data": { histograms.amountOfLoopsInMethods.asJsArray() },
			"fields_per_class_data": { histograms.amountOfFieldsInClasses.asJsArray() },
			"methods_per_class_data": { histograms.amountOfMethodsInClasses.asJsArray() },
			"class_complexity": { histograms.classIndentDepth.asJsArray() },
	])
}

def accumulate() {
	def files = ["asm-histogram.json", "commons-collections4-histogram.json", "google-collections-histogram.json",
			"hamcrest-histogram.json", "jmock-histogram.json", "junit-histogram.json", "mockito-histogram.json",
			"testng-histogram.json", "trove4j-histogram.json", "xstream-histogram.json"]
	def histograms = files.collect{
		new ProjectHistograms().loadFrom("/Users/dima/Library/Application Support/IntelliJIdea12/live-plugins/histograms/data/${it}")
	}
	ProjectHistograms accumulatedHistograms = histograms.inject(new ProjectHistograms()){ result, histogram ->
		result.addAllFrom(histogram)
	}
	accumulatedHistograms.persistHistogramsTo(pathToDataFor("accumulated"))
	fillTemplateWith(accumulatedHistograms, "accumulated")

	show("accumulated")
}

def createGitHubPages() {
	def files = ["idea-histogram.json", "NetBeans-histogram.json", "asm-histogram.json", "commons-collections4-histogram.json",
			"google-collections-histogram.json", "hamcrest-histogram.json", "jmock-histogram.json", "junit-histogram.json",
			"mockito-histogram.json", "testng-histogram.json", "trove4j-histogram.json", "xstream-histogram.json"]
	def histograms = files.collect{
		new ProjectHistograms().loadFrom("/Users/dima/Library/Application Support/IntelliJIdea12/live-plugins/histograms/data/${it}")
	}
	def projectNames = files.collect{ it[0..it.lastIndexOf("-")-1] }
	[projectNames, histograms].transpose().each{ projectName, projectHistograms ->
		fillTemplateWith(projectHistograms, projectName, "${pluginPath()}/gh_pages", "histogram.html")
	}
}