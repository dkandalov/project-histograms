package histograms

/**
 * Calculates Whitespace Integrated over Lines of Text (WILT).
 * (Idea by Robert Smallshire, as described in these slides http://goo.gl/hF1DeF
 * and this talk https://vimeo.com/user22258446/review/79099671/d12d153d71)
 */
class WiltComplexity {
	static int indentDepthOf(String javaCode, int spacesInTab = 2) {
		indentDepthByLineOf(javaCode, spacesInTab).sum(0){ it[0] } as int
	}

	static List indentDepthByLineOf(String javaCode, int spacesInTab = 2) {
		def linesWithIndent = javaCode
				.split("\n").findAll{ !it.trim().empty }
				.collect{ [indentationOf(it, spacesInTab), it] }

		// the idea is to ignore shift for all fields/methods inside class (obviously this is a rough approximation)
		def indents = linesWithIndent.collect{ it[0] }.unique()
		int commonIndent = (indents.size() < 2 ? 0 : indents.sort()[1]) as int
		linesWithIndent.collect{ [atLeastZero(it[0] - commonIndent) / spacesInTab, it[1]] }
	}

	private static int indentationOf(String line, int spacesInTab) {
		line = line.replaceAll("\t", " " * spacesInTab)
		int i = 0
		while (i < line.length() && line[i] == ' ') i++
		i
	}

	private static int atLeastZero(n) {
		n < 0 ? 0 : n
	}
}
