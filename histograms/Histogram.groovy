package histograms

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import templates.HtmlUtil

class Histogram {
	final TreeMap map = new TreeMap()

	def add(int value) {
		if (map[value] == null) map[value] = 0
		map[value] = map[value] + 1
	}

	int size() {
		map.size()
	}

	def toJson() { JsonOutput.toJson(map) }

	def fromJson(String json) {
		def loadedMap = (Map) new JsonSlurper().parseText(json)
				.collectEntries{ [Integer.parseInt(it.key), it.value] }
		map.clear()
		map.putAll(loadedMap)
	}

	String asJsArray() { HtmlUtil.asJsArray(map) }
}
