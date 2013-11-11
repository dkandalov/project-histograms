package histograms

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import templates.HtmlUtil

class Histogram {
	private final TreeMap<Integer, Integer> map = new TreeMap()
	final TreeMap<Integer, Object> maxItemsByValue = new TreeMap()

	def add(int value, int frequency = 1, item = null) {
		if (map[value] == null) map[value] = 0
		map[value] = map[value] + frequency

		addToMaxValues(item, value)
	}

	def addAll(Collection<Integer> values, item) {
		values.each{ value ->
			add(value, 1, item)
		}
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

	private addToMaxValues(item, int value) {
		if (item == null) return
		if (maxItemsByValue.size() <= 15 || value > maxItemsByValue.firstKey()) {
			maxItemsByValue.put(value, item)
			if (maxItemsByValue.size() > 15) maxItemsByValue.remove(maxItemsByValue.firstKey())
		}
	}
}
