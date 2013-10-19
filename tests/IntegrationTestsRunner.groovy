package tests
import com.intellij.openapi.project.Project
import liveplugin.PluginUtil
import org.junit.Ignore
import org.junit.Test

class IntegrationTestsRunner {
	static def runIntegrationTests(Project project, List testClasses) {
		testClasses.each{ runTestsInClass(it, project) }
	}

	private static void runTestsInClass(Class testClass, Project project) {
		def testMethods = testClass.declaredMethods.findAll{
			it.annotations.find{ it instanceof Test } != null && it.annotations.find{ it instanceof Ignore } == null
		}
		def testsResults = testMethods.collect{ method ->
			runTest(method.name, testClass.simpleName, {
				method.invoke(testClass.newInstance())
			})
		}
		PluginUtil.showInConsole(testsResults.join("\n-----\n\n"), "Integration tests", project)
	}

	private static String runTest(String methodName, String className, Closure test) {
		try {

			test()
			"${className}: ${methodName} - OK"

		} catch (Throwable t) {
			def writer = new StringWriter()
			t.printStackTrace(new PrintWriter(writer))
			"${className}: ${methodName} - ERROR\n" + writer.buffer.toString()
		}
	}
}
