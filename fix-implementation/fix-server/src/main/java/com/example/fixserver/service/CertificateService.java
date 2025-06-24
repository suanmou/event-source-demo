package com.example.fixserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import quickfix.SessionID;
import quickfix.fix44.Logon;

import javax.net.ssl.SSLSocket;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CertificateService {

    private static final String KEY_STORE_TYPE = "JKS";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 4096; // 推荐使用4096位
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String CERTIFICATE_TYPE = "X.509";
    private static final int CERTIFICATE_VALIDITY_DAYS = 365 * 2; // 证书有效期2年

    // 存储会话ID与证书的映射关系
    private final Map<String, X509Certificate> sessionCertificates = new HashMap<>();

    @Autowired
    private KeyStore keyStore;

    /**
     * 为新的FIX会话生成证书
     */
    public Map<String, String> generateCertificateForSession(SessionID sessionID) throws Exception {
        String sessionKey = sessionID.toString();
        
        // 生成RSA密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // 创建证书
        X509Certificate certificate = createCertificate(sessionKey, keyPair);
        
        // 保存证书到密钥库
        String alias = "client-" + UUID.randomUUID().toString();
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), "password".toCharArray(), 
                             new Certificate[] { certificate });
        
        // 保存密钥库到文件
        saveKeyStore();
        
        // 存储会话与证书的映射关系
        sessionCertificates.put(sessionKey, certificate);
        
        // 返回证书信息给客户端
        return Map.of(
            "alias", alias,
            "validFrom", certificate.getNotBefore().toString(),
            "validTo", certificate.getNotAfter().toString(),
            "fingerprint", getCertificateFingerprint(certificate)
        );
    }

    /**
     * 创建自签名X.509证书
     */
    private X509Certificate createCertificate(String sessionKey, KeyPair keyPair) throws Exception {
        // 这里简化了证书生成逻辑，实际应用中应该使用更安全的方法
        // 例如使用Bouncy Castle库生成符合X.509标准的证书
        
        // 设置证书有效期
        LocalDate now = LocalDate.now();
        Date startDate = Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(now.plusDays(CERTIFICATE_VALIDITY_DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        // 简化的证书生成逻辑
        // 实际应用中需要使用CertificateFactory或其他库来创建完整的X.509证书
        X509Certificate certificate = new MockX509Certificate(); // 这是一个简化的模拟证书
        
        // 设置证书属性
        certificate.setNotBefore(startDate);
        certificate.setNotAfter(endDate);
        certificate.setPublicKey(keyPair.getPublic());
        certificate.setSerialNumber(generateSerialNumber());
        
        return certificate;
    }

    /**
     * 从SSL连接中获取客户端证书并验证
     */
    public boolean validateClientCertificate(SSLSocket socket, SessionID sessionID) {
        try {
            // 获取客户端证书链
            Certificate[] clientCerts = socket.getSession().getPeerCertificates();
            
            if (clientCerts.length == 0) {
                return false;
            }
            
            // 验证证书链
            X509Certificate clientCert = (X509Certificate) clientCerts[0];
            
            // 验证证书是否过期
            Date now = new Date();
            if (clientCert.getNotBefore().after(now) || clientCert.getNotAfter().before(now)) {
                return false;
            }
            
            // 验证证书密钥长度
            if (clientCert.getPublicKey().getEncoded().length < 2048 / 8) {
                return false;
            }
            
            // 存储会话与证书的映射关系
            sessionCertificates.put(sessionID.toString(), clientCert);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取证书指纹
     */
    private String getCertificateFingerprint(X509Certificate certificate) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(certificate.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X:", b));
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 生成证书序列号
     */
    private BigInteger generateSerialNumber() {
        return new BigInteger(160, new SecureRandom());
    }

    /**
     * 保存密钥库到文件
     */
    private void saveKeyStore() throws IOException {
        try (FileOutputStream fos = new FileOutputStream("src/main/resources/server_keystore.jks")) {
            keyStore.store(fos, "password".toCharArray());
        }
    }

    /**
     * 简化的X.509证书模拟类
     * 实际应用中应该使用标准的X509Certificate实现
     */
    private static class MockX509Certificate extends X509Certificate {
        private Date notBefore;
        private Date notAfter;
        private PublicKey publicKey;
        private BigInteger serialNumber;

        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
            Date now = new Date();
            if (notBefore.after(now)) {
                throw new CertificateNotYetValidException();
            }
            if (notAfter.before(now)) {
                throw new CertificateExpiredException();
            }
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
            if (notBefore.after(date)) {
                throw new CertificateNotYetValidException();
            }
            if (notAfter.before(date)) {
                throw new CertificateExpiredException();
            }
        }

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public BigInteger getSerialNumber() {
            return serialNumber;
        }

        @Override
        public Principal getIssuerDN() {
            return new Principal() {
                @Override
                public String getName() {
                    return "CN=Bloomberg FIX Server, O=Bloomberg, C=US";
                }
            };
        }

        @Override
        public Principal getSubjectDN() {
            return new Principal() {
                @Override
                public String getName() {
                    return "CN=Bloomberg FIX Client, O=Client Organization, C=US";
                }
            };
        }

        @Override
        public Date getNotBefore() {
            return notBefore;
        }

        @Override
        public Date getNotAfter() {
            return notAfter;
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return SIGNATURE_ALGORITHM;
        }

        @Override
        public String getSigAlgOID() {
            return "1.2.840.113549.1.1.11";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getKeyUsage() {
            return new boolean[0];
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
            // 简化的验证逻辑
        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
            // 简化的验证逻辑
        }

        @Override
        public String toString() {
            return "MockX509Certificate{" +
                    "issuer='" + getIssuerDN().getName() + '\'' +
                    ", subject='" + getSubjectDN().getName() + '\'' +
                    ", notBefore=" + notBefore +
                    ", notAfter=" + notAfter +
                    '}';
        }

        @Override
        public PublicKey getPublicKey() {
            return publicKey;
        }

        public void setNotBefore(Date notBefore) {
            this.notBefore = notBefore;
        }

        public void setNotAfter(Date notAfter) {
            this.notAfter = notAfter;
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

        public void setSerialNumber(BigInteger serialNumber) {
            this.serialNumber = serialNumber;
        }
    }
}
