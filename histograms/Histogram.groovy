package histograms

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import templates.HtmlUtil

class Histogram {
	final TreeMap<Integer, Integer> map = new TreeMap()

	def add(int value, int frequency = 1) {
		if (map[value] == null) map[value] = 0
		map[value] = map[value] + frequency
	}

	def addAll(Collection<Integer> values) {
		values.each{ add(it) }
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

	def addAllFrom(Histogram histogram) {
		histogram.map.each{ add(it.key, it.value) }
	}
}
