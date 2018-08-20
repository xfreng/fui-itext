package org.fui.sign;

import com.itextpdf.awt.AsianFontMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;


public class SignHighPdf {

    /**
     * @param password     秘钥密码
     * @param keyStorePath 秘钥文件路径
     * @param signPdfSrc   签名的PDF文件
     * @param x
     * @param y
     * @return
     */
    public static byte[] sign(String password, String keyStorePath, String signPdfSrc,
                              float x, float y,
                              String signText) {
        File signPdfSrcFile = new File(signPdfSrc);
        PdfReader reader = null;
        ByteArrayOutputStream signPDFData = null;
        PdfStamper stp = null;
        FileInputStream fos = null;
        try {
            BouncyCastleProvider provider = new BouncyCastleProvider();
            Security.addProvider(provider);
            KeyStore ks = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
            fos = new FileInputStream(keyStorePath);
            ks.load(fos, password.toCharArray()); // 私钥密码
            String alias = ks.aliases().nextElement();
            PrivateKey key = (PrivateKey) ks.getKey(alias, password.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);
            reader = new PdfReader(signPdfSrc);
            signPDFData = new ByteArrayOutputStream();
            // 临时pdf文件
            File temp = new File(signPdfSrcFile.getParent(), System.currentTimeMillis() + ".pdf");
            stp = PdfStamper.createSignature(reader, signPDFData, '\0', temp, true);
            PdfSignatureAppearance sap = stp.getSignatureAppearance();
            sap.setReason("数字签名，不可改变");
            // 是对应x轴和y轴坐标
            sap.setVisibleSignature(new Rectangle(x, y, x + 150, y + 65), 1,
                    "sr" + String.valueOf(System.nanoTime()));
            /////////////////layer 0 Creating the appearance for layer 0
            PdfTemplate n0 = sap.getLayer(0);
            n0.reset();
            float lx = n0.getBoundingBox().getLeft();
            float by = n0.getBoundingBox().getBottom();
            float width = n0.getBoundingBox().getWidth();
            float height = n0.getBoundingBox().getHeight();
            n0.setRGBColorFill(255, 0, 0);
            n0.rectangle(lx, by, 5, height);
            n0.rectangle(lx, by, width, 5);
            n0.rectangle(lx, by + height - 5, width, 5);
            n0.rectangle(lx + width - 5, by, 5, height);
            n0.fill();
            ///////////////////////layer 2
            PdfTemplate n2 = sap.getLayer(2);
            n2.setCharacterSpacing(0.0f);
            ColumnText ct = new ColumnText(n2);
            ct.setSimpleColumn(n2.getBoundingBox());
            n2.setRGBColorFill(255, 0, 0);
            //做一个占位的动作
            Paragraph p1 = new Paragraph(" ");
            BaseFont bf = BaseFont.createFont(AsianFontMapper.ChineseSimplifiedFont, AsianFontMapper.ChineseSimplifiedEncoding_H,
                    BaseFont.NOT_EMBEDDED);
            Font font1 = new Font(bf, 5, Font.BOLD, BaseColor.RED);
            Font font2 = new Font(bf, 13, Font.BOLD, BaseColor.RED);
            p1.setFont(font1);
            ct.addElement(p1);
            Paragraph p = new Paragraph(signText);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setFont(font2);
            ct.addElement(p);
            ct.go();
            stp.getWriter().setCompressionLevel(PdfStream.BEST_COMPRESSION);
            ExternalDigest digest = new BouncyCastleDigest();
            ExternalSignature signature = new PrivateKeySignature(key, DigestAlgorithms.SHA512, provider.getName());
            MakeSignature.signDetached(sap, digest, signature, chain, null, null, null, 0, CryptoStandard.CADES);
            stp.close();
            reader.close();
            return signPDFData.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (signPDFData != null) {
                try {
                    signPDFData.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        //对已经签章的signed.pdf文件再次签章，这次是高清签章
        byte[] fileData = sign("123456", "E:/keystore.p12", "E:/1643-filled.pdf", 350, 290, "电子签章测试\n2018-08-17");
        if (fileData != null) {
            FileOutputStream f = new FileOutputStream(new File("E:/signed2.pdf"));
            f.write(fileData);
            f.close();
        }
    }

}

