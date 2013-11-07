package histograms

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile

class ProjectHistograms {
	final def amountOfMethodsInClasses = new Histogram()
	final def amountOfFieldsInClasses = new Histogram()
	final def amountOfParametersInMethods = new Histogram()
	final def amountOfIfsInMethods = new Histogram()
	final def amountOfLoopsInMethods = new Histogram()
	final def allHistograms = [
			amountOfMethodsInClasses, amountOfFieldsInClasses, amountOfParametersInMethods,
			amountOfIfsInMethods, amountOfLoopsInMethods]


	ProjectHistograms process(Iterator<PsiFileSystemItem> items) {
		for (PsiFileSystemItem item : items) {
			if (item == null || !(item instanceof PsiJavaFile)) continue
			item = (PsiJavaFile) item

			new PsiStats(item).with {
				amountOfMethodsInClasses.add(amountOfMethods, 1, item)
				amountOfFields.each{ amountOfFieldsInClasses.add(it.value, 1, it.key) }
				amountOfParametersInMethods.addAll(amountOfParametersPerMethod, item)
				amountOfIfsInMethods.addAll(amountOfIfStatementsPerMethod, item)
				amountOfLoopsInMethods.addAll(amountOfLoopsPerMethod, item)
			}
		}
		this
	}

	ProjectHistograms addAllFrom(ProjectHistograms otherHistograms) {
		allHistograms.eachWithIndex{ Histogram histogram, int i ->
			histogram.addAllFrom(otherHistograms.allHistograms[i])
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

	Map<String, Map> getMaxValueItems() {
		[
				"Amount of fields in classes": amountOfFieldsInClasses.maxItemsByValue,
				"Amount of methods in classes": amountOfMethodsInClasses.maxItemsByValue,
				"Amount of parameters in methods": amountOfParametersInMethods.maxItemsByValue,
				"Amount of ifs in methods": amountOfIfsInMethods.maxItemsByValue,
				"Amount of loops in methods": amountOfLoopsInMethods.maxItemsByValue
		]
	}

}
