import java.io.*;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.digests.*;
import org.bouncycastle.crypto.signers.*;
import org.bouncycastle.crypto.util.PublicKeyFactory;

/** Fail-closed authentication for the bundled Google roots and DNS server names. */
final class TlsTrust {
    private static org.bouncycastle.asn1.x509.Certificate[] roots;
    static synchronized org.bouncycastle.asn1.x509.Certificate[] roots() throws IOException {
        if(roots!=null)return roots;
        org.bouncycastle.asn1.x509.Certificate[] loaded=new org.bouncycastle.asn1.x509.Certificate[4];
        for(int i=0;i<4;i++){
            InputStream input=TlsTrust.class.getResourceAsStream("/GTS-Root-R"+(i+1)+".der");
            if(input==null)throw new IOException("TLS: thiếu chứng chỉ gốc trong bộ cài.");
            try{loaded[i]=org.bouncycastle.asn1.x509.Certificate.getInstance(new ASN1InputStream(input).readObject());}finally{input.close();}
        }
        roots=loaded;return roots;
    }
    static boolean dnsMatches(String host,String pattern){
        host=host.toLowerCase();pattern=pattern.toLowerCase();
        if(pattern.indexOf('*')<0)return host.equals(pattern);
        if(!pattern.startsWith("*.")||pattern.indexOf('*',1)>=0)return false;
        String suffix=pattern.substring(1);int prefix=host.length()-suffix.length();
        return prefix>0&&host.endsWith(suffix)&&host.substring(0,prefix).indexOf('.')<0;
    }
    static void hostname(org.bouncycastle.asn1.x509.Certificate cert,String host)throws IOException{
        Extensions ex=cert.getTBSCertificate().getExtensions();
        if(ex!=null){GeneralNames names=GeneralNames.fromExtensions(ex,Extension.subjectAlternativeName);
            if(names!=null){GeneralName[] entries=names.getNames();for(int i=0;i<entries.length;i++)
                if(entries[i].getTagNo()==GeneralName.dNSName&&dnsMatches(host,DERIA5String.getInstance(entries[i].getName()).getString()))return;}}
        throw new IOException("TLS: tên máy chủ không khớp chứng chỉ.");
    }
    static void dates(org.bouncycastle.asn1.x509.Certificate cert,long now)throws IOException{
        if(now<cert.getStartDate().getDate().getTime()||now>cert.getEndDate().getDate().getTime())
            throw new IOException("TLS: chứng chỉ chưa/hết hiệu lực. Kiểm tra ngày giờ máy.");
    }
    static void extensions(org.bouncycastle.asn1.x509.Certificate cert,boolean ca,int below)throws IOException{
        Extensions ex=cert.getTBSCertificate().getExtensions();
        if(ex==null){if(ca)throw new IOException("TLS: CA thiếu ràng buộc.");return;}
        ASN1ObjectIdentifier[] critical=ex.getCriticalExtensionOIDs();
        for(int i=0;i<critical.length;i++)if(!critical[i].equals(Extension.basicConstraints)&&!critical[i].equals(Extension.keyUsage)
            &&!critical[i].equals(Extension.subjectAlternativeName)&&!critical[i].equals(Extension.extendedKeyUsage))
            throw new IOException("TLS: phần mở rộng chứng chỉ chưa hỗ trợ.");
        BasicConstraints bc=BasicConstraints.fromExtensions(ex);
        if(ca){if(bc==null||!bc.isCA()||bc.getPathLenConstraint()!=null&&bc.getPathLenConstraint().compareTo(gunny.tls.compat.math.BigInteger.valueOf(below))<0)
                throw new IOException("TLS: chuỗi CA không hợp lệ.");}
        else if(bc!=null&&bc.isCA())throw new IOException("TLS: chứng chỉ máy chủ là CA.");
        KeyUsage usage=KeyUsage.fromExtensions(ex);
        if(usage!=null&&!usage.hasUsages(ca?KeyUsage.keyCertSign:KeyUsage.digitalSignature))throw new IOException("TLS: khóa không được phép sử dụng.");
        ExtendedKeyUsage eku=ExtendedKeyUsage.fromExtensions(ex);
        if(eku!=null&&!eku.hasKeyPurposeId(KeyPurposeId.id_kp_serverAuth)&&!eku.hasKeyPurposeId(KeyPurposeId.anyExtendedKeyUsage))
            throw new IOException("TLS: chứng chỉ không cho phép máy chủ HTTPS.");
    }
    static void signature(org.bouncycastle.asn1.x509.Certificate cert,org.bouncycastle.asn1.x509.Certificate issuer)throws IOException{
        if(!cert.getIssuer().equals(issuer.getSubject())||!cert.getSignatureAlgorithm().equals(cert.getTBSCertificate().getSignature()))
            throw new IOException("TLS: chuỗi chứng chỉ không khớp.");
        String oid=cert.getSignatureAlgorithm().getAlgorithm().getId();Digest digest;boolean rsa;
        if(oid.equals("1.2.840.113549.1.1.11")){digest=new SHA256Digest();rsa=true;}
        else if(oid.equals("1.2.840.113549.1.1.12")){digest=new SHA384Digest();rsa=true;}
        else if(oid.equals("1.2.840.113549.1.1.13")){digest=new SHA512Digest();rsa=true;}
        else if(oid.equals("1.2.840.10045.4.3.2")){digest=new SHA256Digest();rsa=false;}
        else if(oid.equals("1.2.840.10045.4.3.3")){digest=new SHA384Digest();rsa=false;}
        else if(oid.equals("1.2.840.10045.4.3.4")){digest=new SHA512Digest();rsa=false;}
        else throw new IOException("TLS: thuật toán chữ ký CA chưa hỗ trợ.");
        Signer signer=rsa?(Signer)new RSADigestSigner(digest):(Signer)new DSADigestSigner(new ECDSASigner(),digest);
        try{signer.init(false,PublicKeyFactory.createKey(issuer.getSubjectPublicKeyInfo()));byte[] bytes=cert.getTBSCertificate().getEncoded("DER");
            signer.update(bytes,0,bytes.length);if(!signer.verifySignature(cert.getSignature().getOctets()))throw new IOException("TLS: chữ ký chứng chỉ không đúng.");
        }catch(RuntimeException e){throw new IOException("TLS: không xác minh được chữ ký CA.");}
    }
    static void verify(org.bouncycastle.crypto.tls.Certificate chain,String host)throws IOException{
        verify(chain,host,roots(),System.currentTimeMillis());
    }
    static void verify(org.bouncycastle.crypto.tls.Certificate chain,String host,org.bouncycastle.asn1.x509.Certificate[] anchors,long now)throws IOException{
        if(chain==null||chain.isEmpty()||chain.getLength()>8)throw new IOException("TLS: thiếu hoặc quá nhiều chứng chỉ.");
        hostname(chain.getCertificateAt(0),host);
        for(int i=0;i<chain.getLength();i++){
            org.bouncycastle.asn1.x509.Certificate cert=chain.getCertificateAt(i);dates(cert,now);extensions(cert,i>0,Math.max(0,i-1));
            if(i>0)signature(chain.getCertificateAt(i-1),cert);
            for(int r=0;r<anchors.length;r++){
                org.bouncycastle.asn1.x509.Certificate root=anchors[r];
                if(cert.getSubject().equals(root.getSubject())&&org.bouncycastle.util.Arrays.areEqual(cert.getSubjectPublicKeyInfo().getEncoded("DER"),root.getSubjectPublicKeyInfo().getEncoded("DER"))){dates(root,now);extensions(root,true,Math.max(0,i-1));return;}
                if(cert.getIssuer().equals(root.getSubject())){dates(root,now);extensions(root,true,i);signature(cert,root);return;}
            }
        }
        throw new IOException("TLS: chuỗi chứng chỉ không đến CA tin cậy của game.");
    }
}
