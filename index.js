const express = require('express');

const PORT = 8080;

const app = express();

app.get('/', (_req, res) => {
    res.send('Hello, world!');
});

app.listen(PORT, () => {
    console.log(`Server started on port ${PORT}`);
});