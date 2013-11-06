function Histogram(rootElement, labels, sizes) {
	var listOfSeries = [];
	var amount = function(d) { return d[0]; };
	var frequency = function(d) { return d[1]; };

	if (sizes == null) {
		sizes = {};
		sizes.margin = {top: 20, right: 20, bottom: 50, left: 50};
		sizes.width = 960;
		sizes.height = 500;
	}
	var margin = sizes.margin;
	var width = sizes.width - margin.left - margin.right;
	var height = sizes.height - margin.top - margin.bottom;

	var colorCategory = d3.scale.category10();
	var seriesColor = function(i) { return d3.rgb(colorCategory(i)).darker(); };
	var percentile = 1;
	var scaleType = {x: "log", y: "log"};
	var interpolation = "basis";

	labels = inferLabelDefaults(labels);

	rootElement.attr("class", "histogram");
	rootElement.selectAll().remove();

	var tooltip = addTooltipTo(rootElement);

	var headerSpan = appendBlockElementTo(rootElement, width);
	headerSpan.append("h4").text(labels.title);

	var svg = rootElement.append("svg")
		.attr("width", width + margin.left + margin.right)
		.attr("height", height + margin.top + margin.bottom);

	var outerThis = this;
	this.update = function() {
		updateHistogram(svg, listOfSeries, interpolation, percentile, scaleType, tooltip);
		return outerThis;
	};
	this.addSeries = function(series, name) {
		listOfSeries.push(series);
		labels.seriesNames.push(name || "series" + labels.seriesNames.length + 1);
		return outerThis;
	};

	var footerSpan = appendBlockElementTo(rootElement, width + margin.left * 2).append("span")
		.style({ "margin-left": "auto", "margin-right": "auto" });

	addInterpolationTypeDropDownTo(footerSpan, interpolation, function(newInterpolation) {
		interpolation = newInterpolation;
		outerThis.update(); // TODO animate
	});
	addPaddingTo(footerSpan);
	addScaleTypeDropDownTo(footerSpan, "Y axis scale: ", scaleType.y, function(newScaleType) {
		scaleType.y = newScaleType;
		outerThis.update(); // TODO animate?
	});
	addPaddingTo(footerSpan);
	addScaleTypeDropDownTo(footerSpan, "X axis scale: ", scaleType.x, function(newScaleType) {
		scaleType.x = newScaleType;
		outerThis.update(); // TODO animate?
	});
	addPaddingTo(footerSpan);
	addPercentileDropDownTo(footerSpan, percentile, function(newPercentile) {
		percentile = newPercentile;
		outerThis.update(); // TODO animate?
	});

	return this;


	function updateHistogram(svg, listOfSeries, interpolation, percentile, scaleType, tooltip) {
		var series = listOfSeries.map(function(series) {
			return takePercentileOf(series, percentile, amount);
		});

		var maxFrequency = flat(d3.max, series, frequency);
		var maxAmount = flat(d3.max, series, amount) + 1; // +1 to include first "0" in range
		// TODO rename barWidth
		var barWidth = atLeast(1, (width / maxAmount) - 1); // -1 to have gap if bars are big, but at least width of 1 if bars are small

		var x, xAxis;
		var y, yAxis;
		if (scaleType.y == "log") {
			y = d3.scale.log().domain([1, maxFrequency]).range([height, 0]);
			yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(numberFormat).tickValues(logRange(maxFrequency, 10));
		} else if (scaleType.y == "linear") {
			y = d3.scale.linear().domain([0, maxFrequency]).range([height, 0]);
			yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(numberFormat).tickValues(range(maxFrequency, 10));
		}
		if (scaleType.x == "log") {
			series = series.map(function(list) {
				return list.filter(function(d) { return amount(d) > 0; });
			});
			x = d3.scale.log().domain([1, maxAmount]).range([0, width]);
			xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(numberFormat).tickValues(logRange(maxAmount, 10));
		} else if (scaleType.x == "linear") {
			x = d3.scale.linear().domain([0, maxAmount]).range([0, width]);
			xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(numberFormat).tickValues(range(maxAmount, 10));
		}


		var svgGroup = init();
		addLineCharts();
		addAxis();
		if (listOfSeries.length > 1) addLegend();

		function init() {
			svg.select("g").remove();
			return svg.append("g")
				.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
		}

		function addAxis() {
			svgGroup.append("g")
				.attr("class", "x axis")
				.attr("transform", "translate(0," + height + ")")
				.call(xAxis)
				.call(xAxisLabel(labels.xAxis))
				.selectAll(".tick").attr("transform", function (d) {
					return "translate(" + (x(d) + halfOf(barWidth)) + "," + 0 + ")";
				});
			svgGroup.append("g")
				.attr("class", "y axis")
				.call(yAxis)
				.call(yAxisLabel(labels.yAxis));
		}

		function addLineCharts() {
			var line = d3.svg.line()
				.interpolate(interpolation)
				.x(function(d) { return x(amount(d)) + halfOf(barWidth); })
				.y(function(d) { return y(frequency(d)); });

			var lineCharts = svgGroup.selectAll(".lineChart")
				.data(series)
				.enter()
				.append("g").attr("class", "lineChart")
				.style("stroke", function(chart, i) { return seriesColor(i); });

			lineCharts
				.append("path")
				.attr("class", "line")
				.attr("d", function(d) { return line(d); });

			lineCharts.selectAll(".circle")
				.data(function(d) { return d; })
				.enter().append("circle")
				.attr("class", "circle")
				.attr("r", 4)
				.attr("cx", function(d) { return x(amount(d)) + halfOf(barWidth); })
				.attr("cy", function(d) { return y(frequency(d)); })
				.call(tooltip.mouseOverHandler(function(d) {
					var shiftForCursor = 12;
					return {
						x: leftOffsetOf(svg) + margin.left + x(amount(d)) + halfOf(barWidth) + shiftForCursor,
						y: topOffsetOf(svg) + y(frequency(d))
					};
				}));
		}

		function addLegend() {
			svgGroup.call(
				d3.legend(
					labels.seriesNames,
					seriesColor, {
						from: d3Unpack(svg.clientLeft),
						to: d3Unpack(svg).clientLeft + d3Unpack(svg).clientWidth
					})
			);
		}
	}

	function xAxisLabel(labelText) {
		return function(axis) {
			return axis.append("text")
				.text(labelText)
				.attr("transform", function(){
					return "translate(" + halfOf(width - this.clientWidth) + ",40)";
				});
		}
	}

	function yAxisLabel(labelText) {
		return function(axis) {
			return axis.append("text")
				.text(labelText)
				.attr("transform", "rotate(-90)")
				.style("text-anchor", "end")
				.attr("y", -40)
				.attr("x", function () {
					return -halfOf(height - this.clientHeight) + margin.top;
				});
		}
	}

	function addTooltipTo(element) {
		var tooltip = element.append("div")
			.attr("class", "tooltip")
			.style("opacity", 0);

		tooltip.mouseOverHandler = function(getRelativeXY) {
			return function(element) {
				element.on("mouseover", function(d) {
					var text = labels.yAxisTooltip + ": " + frequency(d) + "<br/>" + labels.xAxisTooltip + ": " + amount(d) ;
					var html = tooltip.html(text)
						.style("opacity", .85)
						.style("position", "absolute");
					var relativeXY = getRelativeXY(d, clientWidthOf(tooltip), clientHeightOf(tooltip));
					html.style("left", relativeXY.x + "px")
						.style("top", relativeXY.y + "px");
				})
					.on("mouseout", function() {
						tooltip.style("opacity", 0);
					});
			};
		};

		return tooltip;
	}
}

function inferLabelDefaults(labels) {
	var defaultTo = function(defaultValue, value) { return value || defaultValue; };
	return {
		title: defaultTo("", labels.title),
		xAxis: defaultTo("Amount", labels.xAxis),
		yAxis: defaultTo("Frequency", labels.yAxis),
		xAxisTooltip: defaultTo("Amount", defaultTo(labels.xAxis, labels.xAxisTooltip)),
		yAxisTooltip: defaultTo("Frequency", defaultTo(labels.yAxis, labels.yAxisTooltip)),
		seriesNames: defaultTo([], labels.seriesNames)
	};
}

function addInterpolationTypeDropDownTo(element, defaultInterpolation, onChange) {
	element.append("label").html("Interpolation: ");
	var dropDown = element.append("select");
	["basis", "linear"].forEach(function(interpolation) {
		var option = dropDown.append("option").attr("value", interpolation).html(interpolation);
		if (defaultInterpolation == interpolation) option.attr("selected", "selected");
	});
	dropDown.on("change", function(){ onChange(this.value); });
}

function addScaleTypeDropDownTo(element, label, defaultScaleType, onChange) {
	element.append("label").html(label);
	var dropDown = element.append("select");
	["log", "linear"].forEach(function(scaleType) {
		var option = dropDown.append("option").attr("value", scaleType).html(scaleType);
		if (defaultScaleType == scaleType) option.attr("selected", "selected");
	});
	dropDown.on("change", function(){ onChange(this.value); });
}

function addPercentileDropDownTo(element, defaultPercentile, onChange) {
	element.append("label").html("Percentile: ");
	var dropDown = element.append("select");

	var choices = [100, 99, 98];
	for (var i = 90; i > 0; i -= 10) {
		choices.push(i);
	}
	choices.forEach(function(i) {
		var option = dropDown.append("option").attr("value", i).html(i);
		if (defaultPercentile * 100 == i) option.attr("selected", "selected");
	});

	dropDown.on("change", function(){ onChange(+this.value / 100); });
}

function halfOf(n) { return n / 2; }
function leftOffsetOf(element) { return d3Unpack(element).offsetLeft; }
function topOffsetOf(element) { return d3Unpack(element).offsetTop; }
function clientWidthOf(element) { return d3Unpack(element).clientWidth; }
function clientHeightOf(element) { return d3Unpack(element).clientHeight; }
function d3Unpack(element) { return (element instanceof Array) ? element[0][0] : element; }

function flat(f, list, accessor) {
	return f(list, function(subList) {
		return f(subList, accessor);
	});
}

function addPaddingTo(element) {
	element.append("span").style({width: "20px", display: "inline-block"});
}

function takePercentileOf(series, percentile, accessor) {
	function valueOf(d) {
		return accessor != null ? accessor(d) : d;
	}
	var i = Math.round((series.length - 1) * percentile);
	return series.filter(function(d) { return valueOf(d) <= valueOf(series[i]); });
}

function range(to, desiredAmountOfSteps) {
	var values = function() {
		var multiples = d3.range(0, 9).map(function(i){ return Math.pow(10, i); });
		return multiples.reduce(function(result, i) {
			result.push(i);
			result.push(2 * i);
			result.push(5 * i);
			return result;
		}, []);
	}();
	function rounded(stepSize) {
		var diffs = values.map(function(i){ return Math.abs(i - stepSize); });
		return values[diffs.indexOf(d3.min(diffs))];
	}

	var stepSize = to / desiredAmountOfSteps;
	var result = d3.range(0, to, rounded(stepSize));

	var last = result[result.length - 1];
	if (last != to) {
		// don't add value before last one if they are close (otherwise they might overlap in UI)
		if ((to - last) < stepSize / 2) result = result.splice(0, result.length - 1);
		result.push(to); // always add last value
	}

	return result;
}

function logRange(to, desiredAmountOfSteps) {
	var result = [to];
	function addMultiplesOf(n) {
		while (n < to) {
			result.push(n);
			n = n * 10;
		}
	}
	addMultiplesOf(1);
	if (result.length < desiredAmountOfSteps) addMultiplesOf(5);
	if (result.length < desiredAmountOfSteps) addMultiplesOf(2);

	result = result.sort(d3.ascending);

	if (result.length >= 2) {
		var last = result[result.length - 1];
		var nextToLast = result[result.length - 2];
		if ((last - nextToLast) / last <= 0.12) {
			result.splice(result.length - 2, 1);
		}
	}
	return result;
}

function atLeast(minValue, value) {
	return value < minValue ? minValue : value;
}

function numberFormat(n) {
	if (!isFinite(n)) return "";
	var s = d3.format(",")(n);

	// use ".2" precision in attempt to avoid overlapping y axis label
	var result = s.length < 4 ? s : d3.format(".2s")(n);

	// workaround for "s" formatting because with ".0" it uses "e" notation
	result = removeFrom(result, ".00");
	result = removeFrom(result, ".0");

	return result;
}

function removeFrom(string, subString) {
	var i = string.indexOf(subString);
	if (i == -1) return string;
	else return string.substring(0, i) + string.substring(i + subString.length, string.length);
}

function appendBlockElementTo(element, width) {
	return element
		.append("span").style({display: "block", "text-align": "center", width: width + "px"});
}