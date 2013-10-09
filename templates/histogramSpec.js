describe("histogram", function () {
	var rootElement;
	var testSeries = [ [0, 1], [1, 2], [2, 2], [3, 1], [4, 1], [5, 5] ];
	beforeEach(function() {
		rootElement = d3.select("body").append("span").attr("id", "test-histogram");
		new Histogram("test-histogram", "Test histogram title").addSeries(testSeries).update();
	});

	it("should be represented as a line chart", function () {
		expect(d3.selectAll(".lineChart path").size()).toEqual(1);
		expect(d3.select(".lineChart path").node().getAttribute("d").length).toBeGreaterThan(0);
	});

	it("and have circles for each data point", function() {
		expect(d3.selectAll(".lineChart circle").size()).toEqual(testSeries.length);
	});

	afterEach(function() {
		d3.select("#test-histogram").remove();
	});
});

describe("number format", function () {
	it("should not be more than 4 characters long", function () {
		expect(numberFormat(1)).toEqual("1");
		expect(numberFormat(1.0)).toEqual("1");
		expect(numberFormat(100)).toEqual("100");
		expect(numberFormat(1000)).toEqual("1k");
		expect(numberFormat(1100)).toEqual("1.1k");
		expect(numberFormat(1234)).toEqual("1.2k");
		expect(numberFormat(10000)).toEqual("10k");
		expect(numberFormat(12345)).toEqual("12k");
		expect(numberFormat(100000)).toEqual("100k");
		expect(numberFormat(1000000)).toEqual("1M");
		expect(numberFormat(1234567)).toEqual("1.2M");
	})
});

describe("range", function() {
	var amountOfSteps = 5;
	it("should choose values between 0 and range end", function() {
		expect(range(1, amountOfSteps)).toEqual([0, 1]);
		expect(range(10, amountOfSteps)).toEqual([0, 2, 4, 6, 8, 10]);
	});
	it("should always include range end", function() {
		expect(range(121, amountOfSteps)).toEqual([0, 20, 40, 60, 80, 100, 121]);
		expect(range(15, amountOfSteps)).toEqual([0, 2, 4, 6, 8, 10, 12, 15]);
	});
	it("should with large numbers", function() {
		expect(range(10 * 1000000, amountOfSteps)).toEqual([ 0, 2000000, 4000000, 6000000, 8000000, 10000000 ]);
	});
});

describe("logRange", function() {
	var amountOfSteps = 5;
	it("should choose multiple-of-ten values between 0 and range end", function() {
		expect(logRange(10, amountOfSteps)).toEqual([1, 2, 5, 10]);
		expect(logRange(20, amountOfSteps)).toEqual([1, 2, 5, 10, 20]);
		expect(logRange(30, amountOfSteps)).toEqual([1, 5, 10, 20, 30]);
		expect(logRange(100, amountOfSteps)).toEqual([1, 10, 50, 100]);
	});
});

describe("percentile", function() {
	it("should leave all elements when percentile is 1", function() {
		var percentile = 1;
		expect(takePercentileOf([], percentile)).toEqual([]);
		expect(takePercentileOf([42], percentile)).toEqual([42]);
		expect(takePercentileOf([1, 2, 3, 4, 5], percentile)).toEqual([1, 2, 3, 4, 5]);
	});
	it("should leave half of elements when percentile is 0.5", function() {
		var percentile = 0.5;
		expect(takePercentileOf([], percentile)).toEqual([]);
		expect(takePercentileOf([42], percentile)).toEqual([42]);
		expect(takePercentileOf([1, 2, 3, 4, 5], percentile)).toEqual([1, 2, 3]);
	});
});