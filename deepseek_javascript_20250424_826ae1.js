class ResultShard {
  constructor(shardSize = 50000) {
    this.shards = [];
    this.currentShard = [];
    this.shardSize = shardSize;
  }

  add(data) {
    this.currentShard.push(data);
    if (this.currentShard.length >= this.shardSize) {
      this.shards.push(this.currentShard);
      this.currentShard = [];
    }
  }

  getAll() {
    return [...this.shards, this.currentShard].flat();
  }
}