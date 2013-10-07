function createHistogram(elementId, histogramTitle, rawData) {
	var listOfData = [rawData.map(function(d) {
		return {amount: d[0], frequency: d[1]};
	})];

	var margin = {top: 20, right: 20, bottom: 50, left: 50},
		width = 960 - margin.left - margin.right,
		height = 500 - margin.top - margin.bottom;
	var percentile = 1;
	var scaleType = "log";
	var interpolation = "basis";

	var rootElement = d3.select("#" + elementId).attr("class", "histogram");
	removeChildrenOf(elementId);

	var tooltip = addTooltipTo(rootElement);

	var headerSpan = appendBlockElementTo(rootElement, width);
	headerSpan.append("h2").text(histogramTitle).style({"text-align": "center"});

	var svg = rootElement.append("svg")
		.attr("width", width + margin.left + margin.right)
		.attr("height", height + margin.top + margin.bottom);

	var update = function() {
		updateHistogram(svg, listOfData, interpolation, percentile, scaleType, tooltip);
	};

	var footerSpan = appendBlockElementTo(rootElement, width + margin.left * 2).append("span")
		.style({ "margin-left": "auto", "margin-right": "auto" });

	addInterpolatonTypeDropDownTo(footerSpan, interpolation, function(newInterpolation) {
		interpolation = newInterpolation;
		update(); // TODO animate
	})
	addPaddingTo(footerSpan);
	addScaleTypeDropDownTo(footerSpan, scaleType, function(newScaleType) {
		scaleType = newScaleType;
		update(); // TODO animate?
	});
	addPaddingTo(footerSpan);
	addPercentileDropDownTo(footerSpan, percentile, function(newPercentile) {
		percentile = newPercentile;
		update(); // TODO animate?
	});

	update();


	function updateHistogram(svg, listOfData, interpolation, percentile, scaleType, tooltip) {
		listOfData = listOfData.map(function(data) {
			return takePercentileOf(data, percentile, function(d){ return d.amount; });
		});

		var maxFrequency = d3.max(listOfData, function(data) {
			return d3.max(data, function(d){return d.frequency;});
		});
		var maxAmount = d3.max(listOfData, function(data) {
			return d3.max(data, function(d){ return d.amount; });
		}) + 1; // +1 to include first "0" in range
		var barWidth = atLeast(1, (width / maxAmount) - 1); // -1 to have gap if bars are big, but at least width of 1 if bars are small

		var x = d3.scale.linear().domain([0, maxAmount]).range([0, width]);
		var xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(numberFormat).tickValues(range(maxAmount, 10));
		var y;
		var yAxis;
		if (scaleType == "log") {
			y = d3.scale.log().domain([1, maxFrequency]).range([height, 0]);
			yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(numberFormat).tickValues(logRange(maxFrequency, 10));
		} else if (scaleType == "linear") {
			y = d3.scale.linear().domain([1, maxFrequency]).range([height, 0]);
			yAxis = d3.svg.axis().scale(y).orient("left").tickFormat(numberFormat).tickValues(range(maxFrequency, 10));
		}

		var svgGroup = init();
		addLineCharts();
		addAxis();


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
				.call(xAxisLabel(histogramTitle))
				.selectAll(".tick").attr("transform", function (d) {
					return "translate(" + (x(d) + halfOf(barWidth)) + "," + 0 + ")";
				});
			svgGroup.append("g")
				.attr("class", "y axis")
				.call(yAxis)
				.call(yAxisLabel("Frequency"));
		}

		function addLineCharts() {
			var line = d3.svg.line()
				.interpolate(interpolation)
				.x(function(d) { return x(d.amount) + halfOf(barWidth); })
				.y(function(d) { return y(d.frequency); });

			var lineCharts = svgGroup.selectAll(".lineChart")
				.data(listOfData)
				.enter()
				.append("g").attr("class", "lineChart");

			lineCharts
				.append("path")
				.attr("class", "line")
				.attr("d", function(d) { return line(d); });

			listOfData.forEach(function(data) {
				svgGroup.selectAll(".circle")
					.data(data)
					.enter().append("circle")
					.attr("class", "circle")
					.attr("r", 4)
					.attr("cx", function(d) { return x(d.amount) + halfOf(barWidth); })
					.attr("cy", function(d) { return y(d.frequency); })
					.call(tooltip.mouseOverHandler(function(d) {
						var shiftForCursor = 12;
						return {
							x: leftOffsetOf(svg) + margin.left + x(d.amount) + halfOf(barWidth) + shiftForCursor,
							y: topOffsetOf(svg) + y(d.frequency)
						};
					}));
			});
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

	function removeChildrenOf(elementId) {
		var element = document.getElementById(elementId);
		for (var i = 0; i < element.children.length; i++) {
			parent.removeChild(element.children.item(i));
		}
	}
}

function addTooltipTo(element) {
	var tooltip = element.append("div")
		.attr("class", "tooltip")
		.style("opacity", 0);

	tooltip.mouseOverHandler = function(getRelativeXY) {
		return function(element) {
			element.on("mouseover", function(d) {
				var html = tooltip.html("Amount: " + d.amount + "<br/>Frequency: " + d.frequency)
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

function addInterpolatonTypeDropDownTo(element, defaultInterpolation, onChange) {
	element.append("label").html("Interpolation: ");
	var dropDown = element.append("select");
	["basis", "linear"].forEach(function(interpolation) {
		var option = dropDown.append("option").attr("value", interpolation).html(interpolation);
		if (defaultInterpolation == interpolation) option.attr("selected", "selected");
	});
	dropDown.on("change", function(){ onChange(this.value); });
}

function addScaleTypeDropDownTo(element, defaultScaleType, onChange) {
	element.append("label").html("Y axis scale: ");
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

function addPaddingTo(element) {
	element.append("span").style({width: "20px", display: "inline-block"});
}

function takePercentileOf(data, percentile, accessor) {
	function valueOf(d) {
		return accessor != null ? accessor(d) : d;
	}
	var i = Math.round((data.length - 1) * percentile);
	return data.filter(function(d) { return valueOf(d) <= valueOf(data[i]); });
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

	if (result.length >= 2) {
		var last = result[result.length - 1];
		var nextToLast = result[result.length - 2];
		if (nextToLast / last <= 0.1) {
			result.splice(result.length - 2, 1);
		}
	}
	return result.sort(d3.ascending);
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