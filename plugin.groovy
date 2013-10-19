import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import histograms.ProjectHistograms
import tests.IntegrationTestsRunner
import tests.PsiStatsTest

import static liveplugin.PluginUtil.*
import static templates.HtmlUtil.createFromTemplate

if (true) return IntegrationTestsRunner.runIntegrationTests(project, [PsiStatsTest])



registerAction("miscProjectHistograms", "ctrl shift H") { AnActionEvent event ->
  def project = event.project
	def accumulatedHistograms = "accumulated"
	def pathToDataFor = { String name -> "${pluginPath()}/data/${name}-histogram.json" }

	showPopupMenu([
			"Build and Show histograms.Histogram": {
				doInBackground("Building histograms"){
					runReadAction{
						def histograms = new ProjectHistograms()
								.process(allPsiItemsIn(project))
								.persistHistogramsTo(pathToDataFor(project.name))
						openInBrowser(fillTemplateFrom(histograms, project.name))
					}
				}
			},
			"Build histograms.Histogram and Accumulate": {
				doInBackground("Building and accumulating histogram"){
					runReadAction{
						new ProjectHistograms()
								.loadFrom(pathToDataFor(accumulatedHistograms))
								.process(allPsiItemsIn(project))
								.persistHistogramsTo(pathToDataFor(accumulatedHistograms))
						show("Accumulated histograms from ${project.name}")
					}
				}
			},
			"Reset Accumulated Data": {
				new File(pathToDataFor(accumulatedHistograms)).delete()
				show("Deleted accumulated data")
			},
			"Show Accumulated histograms.Histogram": {
				def histograms = new ProjectHistograms()
						.loadFrom(pathToDataFor(accumulatedHistograms))
				openInBrowser(fillTemplateFrom(histograms, accumulatedHistograms))
			}
	], "Histograms")
}
show("reloaded")


String pluginPath() { pluginPath }

static openInBrowser(File file) { BrowserUtil.open("file://${file.absolutePath}") }

File fillTemplateFrom(ProjectHistograms histograms, String name) {
	createFromTemplate("${pluginPath()}/templates", "histogram.html", name, [
			"project_name_placeholder": { name },
			"parameters_per_method_data": { histograms.amountOfParametersInMethods.asJsArray() },
			"ifs_per_method_data": { histograms.amountOfIfsInMethods.asJsArray() },
			"fields_per_class_data": { histograms.amountOfFieldsInClasses.asJsArray() },
			"methods_per_class_data": { histograms.amountOfMethodsInClasses.asJsArray() },
	])
}

