// Originally by ziggy.jonsson.nyc@gmail.com (MIT licence)
(function() {
	var items = {};
	var createLegend = function(g) {
		g.each(function() {
			var g = d3.select(this),
				legendPadding = g.attr("data-style-padding") || 5,
				legendBox = g.selectAll(".legend-box").data([true]),
				legendItems = g.selectAll(".legend-items").data([true]);

			legendBox.enter().append("rect").classed("legend-box", true);
			legendItems.enter().append("g").classed("legend-items", true);

			legendItems.selectAll("text")
				.data(items, function(d) { return d.key; })
				.call(function(d) { d.enter().append("text"); })
				.call(function(d) { d.exit().remove(); })
				.attr("y", function(d, i) { return i + "em"; })
				.attr("x", "1em")
				.text(function(d) { return d.key; });

			legendItems.selectAll("circle")
				.data(items, function(d) { return d.key; })
				.call(function(d) { d.enter().append("circle"); })
				.call(function(d) { d.exit().remove(); })
				.attr("cy", function(d, i) { return i - 0.25 + "em"; })
				.attr("cx", 0)
				.attr("r", "0.4em")
				.style("fill", function(d) { return d.value.color; });

			// Reposition and resize the box
			var lbbox = legendItems[0][0].getBBox();
			legendBox.attr("x", (lbbox.x - legendPadding))
				.attr("y", (lbbox.y - legendPadding))
				.attr("height", (lbbox.height + 2 * legendPadding))
				.attr("width", (lbbox.width + 2 * legendPadding))
		});
		return g
	};
	d3.legend = function(seriesNames, itemColor) {
		items = {};
		seriesNames.forEach(function(d, i) { items[d] = {color: itemColor(i)} });
		items = d3.entries(items);

		return createLegend;
	};
})();