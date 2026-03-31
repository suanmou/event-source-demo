app.post('/tests/:id/stop', (req, res) => {
  const test = testStore.get(req.params.id);
  if (!test) return res.status(404).json({ error: 'Test not found' });

  if (test.worker && test.worker.terminate) {
    test.worker.terminate();
    test.status = 'stopped';
    res.json({ status: 'stopped' });
  } else {
    res.status(400).json({ error: 'Test cannot be stopped' });
  }
});