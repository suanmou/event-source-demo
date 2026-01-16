<el-dialog title="历史测试记录" :visible.sync="historyVisible">
  <el-table :data="testHistory">
    <el-table-column prop="id" label="测试ID"></el-table-column>
    <el-table-column prop="status" label="状态"></el-table-column>
    <el-table-column label="操作">
      <template slot-scope="scope">
        <el-button @click="loadHistoryReport(scope.row.id)">查看</el-button>
      </template>
    </el-table-column>
  </el-table>
</el-dialog>