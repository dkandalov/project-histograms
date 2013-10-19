package histograms

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile

class ProjectHistograms {
	final def amountOfMethodsInClasses = new Histogram()
	final def amountOfFieldsInClasses = new Histogram()
	final def amountOfParametersInMethods = new Histogram()
	final def amountOfIfsInMethods = new Histogram()
	final def allHistograms = [amountOfMethodsInClasses, amountOfFieldsInClasses, amountOfParametersInMethods]


	ProjectHistograms process(Iterator<PsiFileSystemItem> items) {
		for (PsiFileSystemItem item : items) {
			if (item == null || !(item instanceof PsiJavaFile)) continue

			def methods = PsiStats.allMethodsIn(item)
			def fields = PsiStats.allFieldsIn(item)

			amountOfFieldsInClasses.add(fields.size())
			amountOfMethodsInClasses.add(methods.size())
			methods.each{ method ->
				amountOfParametersInMethods.add(PsiStats.amountOfParametersIn(method))
				amountOfIfsInMethods.add(PsiStats.amountOfIfStatementsIn(method))
			}
		}
		this
	}

	ProjectHistograms persistHistogramsTo(String filePath) {
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
