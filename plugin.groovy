import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import histograms.ProjectHistograms
import tests.IntegrationTestsRunner
import tests.PsiStatsTest

import static com.intellij.openapi.util.text.StringUtil.capitalize
import static liveplugin.PluginUtil.*
import static templates.HtmlUtil.createFromTemplate

if (false) return IntegrationTestsRunner.runIntegrationTests(project, [PsiStatsTest])
if (false) return accumulate()

registerAction("miscProjectHistograms", "ctrl shift H") { AnActionEvent event ->
  def project = event.project

	doInBackground("Building histograms"){
		runReadAction{
			def histograms = new ProjectHistograms()
					.process(allPsiItemsIn(project))
					.persistHistogramsTo(pathToDataFor(project.name))
			openInBrowser(fillTemplateWith(histograms, project.name))
		}
	}
}
show("reloaded")


String pluginPath() { pluginPath }

String pathToDataFor(String name) { "${pluginPath()}/data/${name}-histogram.json" }

static openInBrowser(File file) { BrowserUtil.open("file://${file.absolutePath}") }

File fillTemplateWith(ProjectHistograms histograms, String name) {
	createFromTemplate("${pluginPath()}/templates", "histogram.html", name, [
			"project_name_placeholder": { capitalize(name) },
			"parameters_per_method_data": { histograms.amountOfParametersInMethods.asJsArray() },
			"ifs_per_method_data": { histograms.amountOfIfsInMethods.asJsArray() },
			"loops_per_method_data": { histograms.amountOfLoopsInMethods.asJsArray() },
			"fields_per_class_data": { histograms.amountOfFieldsInClasses.asJsArray() },
			"methods_per_class_data": { histograms.amountOfMethodsInClasses.asJsArray() },
	])
}

def accumulate() {
	def files = ["asm-histogram.json", "commons-collections4-histogram.json", "google-collections-read-only-histogram.json",
			"JavaHamcrest-histogram.json", "jmock-library-histogram.json", "junit-histogram.json", "mockito-histogram.json",
			"testng-histogram.json", "trove4j-histogram.json", "xstream-parent-histogram.json"]
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
