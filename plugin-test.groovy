import liveplugin.testrunner.IntegrationTestsRunner
import tests.ComplexityTest
import tests.PsiStatsTest

IntegrationTestsRunner.runIntegrationTests([PsiStatsTest, ComplexityTest], project, pluginPath)
