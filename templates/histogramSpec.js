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
	});
});
