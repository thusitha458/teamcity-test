describe("Teamcity Test", () => {
    it('dummy test', (done) => {
        if (5 === 5) {
            done();
        } else {
            done(new Error("Not equal!"));
        }
    })

    it('another dummy test', (done) => {
        if (6 === 6) {
            done();
        } else {
            done(new Error("Not equal!!!"));
        }
    })
});