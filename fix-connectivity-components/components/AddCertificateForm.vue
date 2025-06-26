<template>
  <el-dialog
    title="添加证书"
    :visible.sync="dialogVisible"
    width="600px"
    :before-close="handleClose"
  >
    <template #content>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="证书名称" prop="name">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="证书类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择证书类型">
            <el-option label="RSA 2048" value="rsa2048"></el-option>
            <el-option label="RSA 4096" value="rsa4096"></el-option>
            <el-option label="ECDSA P-256" value="ecdsa256"></el-option>
            <el-option label="ECDSA P-384" value="ecdsa384"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="连接" prop="connectionId">
          <el-select v-model="form.connectionId" placeholder="请选择连接">
            <el-option v-for="connection in connections" :key="connection.id" :label="connection.name" :value="connection.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="证书内容" prop="certificateContent">
          <el-input type="textarea" :rows="10" v-model="form.certificateContent"></el-input>
        </el-form-item>
        <el-form-item label="私钥内容" prop="privateKeyContent">
          <el-input type="textarea" :rows="10" v-model="form.privateKeyContent"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description"></el-input>
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'AddCertificateForm',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    connections: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      dialogVisible: this.visible,
      form: {
        name: '',
        type: '',
        connectionId: '',
        certificateContent: '',
        privateKeyContent: '',
        description: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入证书名称', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择证书类型', trigger: 'change' }
        ],
        connectionId: [
          { required: true, message: '请选择连接', trigger: 'change' }
        ],
        certificateContent: [
          { required: true, message: '请输入证书内容', trigger: 'blur' }
        ],
        privateKeyContent: [
          { required: true, message: '请输入私钥内容', trigger: 'blur' }
        ]
      }
    };
  },
  watch: {
    visible(newVal) {
      this.dialogVisible = newVal;
      if (newVal) {
        this.resetForm();
      }
    }
  },
  methods: {
    resetForm() {
      this.form = {
        name: '',
        type: '',
        connectionId: '',
        certificateContent: '',
        privateKeyContent: '',
        description: ''
      };
    },
    handleClose(done) {
      this.$confirm('确认关闭？')
        .then(() => {
          done();
          this.$emit('update:visible', false);
        })
        .catch(() => {});
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (valid) {
          this.$emit('submit', this.form);
          this.dialogVisible = false;
          this.$emit('update:visible', false);
        } else {
          console.log('error submit!!');
          return false;
        }
      });
    }
  }
};
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
    