package templates

import java.util.regex.Matcher

class HtmlUtil {
	static File createFromTemplate(String templateFolder, String template, String projectName, Map placeHoldersMapping) {
		def templateText = new File("$templateFolder/$template").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("$templateFolder/$fileName").readLines().join("\n") }
		text = placeHoldersMapping.inject(text) { result, entry ->
			fillPlaceholder(result, entry.value(), entry.key)
		}
		def file = new File("${templateFolder}/${projectName}_${template}")
		file.write(text)
		file
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
		templateText
				.replaceFirst(/(?s)\/\*${placeHolder}\*\/.*\/\*${placeHolder}\*\//, Matcher.quoteReplacement(jsValue))
				.replaceFirst(/(?s)\[${placeHolder}\]/, Matcher.quoteReplacement(jsValue))
	}

	static String asJsArray(Map map) {
		"[" + map.entrySet().collect{"[${it.key},${it.value}]"}.join(",") + "]"
	}
}
