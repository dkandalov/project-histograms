import liveplugin.testrunner.IntegrationTestsRunner
import tests.IndentDepthTest
import tests.PsiStatsTest

IntegrationTestsRunner.runIntegrationTests([PsiStatsTest, IndentDepthTest], project, pluginPath)
