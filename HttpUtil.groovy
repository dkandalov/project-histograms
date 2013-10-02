import java.util.regex.Matcher

class HttpUtil {
	static void createFromTemplate(String templateFolder, String template, String projectName, Map placeHoldersMapping) {
		def templateText = new File("$templateFolder/$template").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("$templateFolder/$fileName").readLines().join("\n") }
		text = placeHoldersMapping.inject(text) { result, entry ->
			fillPlaceholder(result, entry.value(), entry.key)
		}
		new File("${templateFolder}/${projectName}_${template}").write(text)
	}

	static String inlineJSLibraries(String html, Closure<String> sourceCodeReader) {
		(html =~ /(?sm).*?<script src="(.*?)"><\/script>.*/).with{
			if (!matches()) html
			else inlineJSLibraries(
					html.replace("<script src=\"${group(1)}\"></script>", "<script>${sourceCodeReader(group(1))}</script>"),
					sourceCodeReader
			)
		}
	}

	static String fillPlaceholder(String templateText, String jsValue, String placeHolder) {
		templateText.replaceFirst(/(?s)\/\*${placeHolder}\*\/.*\/\*${placeHolder}\*\//, Matcher.quoteReplacement(jsValue))
	}

	static String asJsArray(List<Integer> list) {
		"[" + list.join(",") + "]"
	}
}
