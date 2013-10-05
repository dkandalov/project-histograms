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