app.get('/tests/:id/requests', (req, res) => {
  const test = testStore.get(req.params.id);
  if (!test) return res.status(404).json({ error: 'Test not found' });

  const page = parseInt(req.query.page) || 1;
  const size = parseInt(req.query.size) || 100;
  const start = (page - 1) * size;
  const end = start + size;

  res.json({
    page,
    totalPages: Math.ceil(test.stats.responseTimes.length / size),
    data: test.stats.responseTimes.slice(start, end)
  });
});