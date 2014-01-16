package histograms

/**
 * Measures amount of indentation in source code with the assumption that
 * it's a good enough proxy of code complexity.
 */
class Complexity {
	static int of(String javaCode, int spacesInTab = 2) {
		def lines = javaCode.split("\n")
		def lineIndents = lines.findAll{ !it.trim().empty }.collect{ indentationOf(it, spacesInTab) }
		// TODO create more detailed method?
//		PluginUtil.show(lines.findAll{ !it.trim().empty }.collect{ [it, indentationOf(it, spacesInTab)] })

		// here the idea is to ignore shift for all field, methods inside class or similar definition
		// (obviously this is rough approximation)
		int commonIndent = new ArrayList<Integer>(lineIndents).unique().sort().tail().first()
		lineIndents.findAll{ it > commonIndent }.sum(0){ it - commonIndent } as int
	}

	private static int indentationOf(String line, int spacesInTab) {
		line = line.replaceAll("\t", " " * spacesInTab)
		int i = 0
		while (i < line.length() && line[i] == ' ') i++
		i
	}
}
