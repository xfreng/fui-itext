package org.fui.sign;

import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfSignatureAppearance.RenderingMode;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.security.*;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fui.model.RectangleCentre;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


public class SignPdf {


    /**
     * @param password     秘钥密码
     * @param keyStorePath 秘钥文件路径
     * @param signPdfSrc   签名的PDF文件
     * @param signImage    签名图片文件
     * @param signKeyword  要签名位置所在的文本
     * @return
     */
    public static byte[] sign(String password, String keyStorePath, String signPdfSrc, String signImage, String signKeyword) {
        return sign(password, keyStorePath, signPdfSrc, signImage, signKeyword, 0, 0);
    }

    /**
     * @param password     秘钥密码
     * @param keyStorePath 秘钥文件路径
     * @param signPdfSrc   签名的PDF文件
     * @param signImage    签名图片文件
     * @param x            x坐标
     * @param y            y坐标
     * @return
     */
    public static byte[] sign(String password, String keyStorePath, String signPdfSrc, String signImage, float x, float y) {
        return sign(password, keyStorePath, signPdfSrc, signImage, null, x, y);
    }

    /**
     * @param password     秘钥密码
     * @param keyStorePath 秘钥文件路径
     * @param signPdfSrc   签名的PDF文件
     * @param signImage    签名图片文件
     * @param signKeyword  要签名位置所在的文本
     * @param x            x坐标
     * @param y            y坐标
     * @return
     */
    public static byte[] sign(String password, String keyStorePath, String signPdfSrc, String signImage, String signKeyword,
                              float x, float y) {
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
            // 私钥密码 为Pkcs生成证书是的私钥密码 123456
            ks.load(fos, password.toCharArray());
            String alias = ks.aliases().nextElement();
            PrivateKey key = (PrivateKey) ks.getKey(alias, password.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);
            reader = new PdfReader(signPdfSrc);
            signPDFData = new ByteArrayOutputStream();
            // 使用png格式透明图片
            Image image = Image.getInstance(signImage);

            if (StringUtils.isNotBlank(signKeyword)) {
                int pageNum = reader.getNumberOfPages();
                // 签章位置对象
                final List<RectangleCentre> rectangleCentreList = new LinkedList<RectangleCentre>();
                final String text = signKeyword;
                // 下标从1开始
                for (int page = 1; page <= pageNum; page++) {
                    final RectangleCentre rectangleCentreBase = new RectangleCentre();
                    rectangleCentreBase.setPage(page);
                    PdfReaderContentParser pdfReaderContentParser = new PdfReaderContentParser(reader);
                    pdfReaderContentParser.processContent(page, new RenderListener() {

                        StringBuilder sb = new StringBuilder();
                        int maxLength = text.length();

                        public void renderText(TextRenderInfo textRenderInfo) {
                            // 只适用 单字块文档 以及 关键字整个为一个块的情况
                            // 设置 关键字文本为单独的块，不然会错位
                            boolean isKeywordChunk = textRenderInfo.getText().length() == maxLength;
                            if (isKeywordChunk) {
                                // 文档按照 块 读取
                                sb.delete(0, sb.length());
                                sb.append(textRenderInfo.getText());
                            } else {
                                // 有些文档 单字一个块的情况
                                // 拼接字符串
                                sb.append(textRenderInfo.getText());
                                // 去除首部字符串，使长度等于关键字长度
                                if (sb.length() > maxLength) {
                                    sb.delete(0, sb.length() - maxLength);
                                }
                            }
                            // 判断是否匹配上
                            if (text.equals(sb.toString())) {
                                RectangleCentre rectangleCentre = rectangleCentreBase.copy();

                                // 计算中心点坐标

                                com.itextpdf.awt.geom.Rectangle2D.Float baseFloat = textRenderInfo.getBaseline()
                                        .getBoundingRectange();
                                com.itextpdf.awt.geom.Rectangle2D.Float ascentFloat = textRenderInfo.getAscentLine()
                                        .getBoundingRectange();

                                float centreX;
                                float centreY;
                                if (isKeywordChunk) {
                                    centreX = baseFloat.x + ascentFloat.width / 2;
                                    centreY = baseFloat.y + ((ascentFloat.y - baseFloat.y) / 2);
                                } else {
                                    centreX = baseFloat.x + ascentFloat.width - (maxLength * ascentFloat.width / 2);
                                    centreY = baseFloat.y + ((ascentFloat.y - baseFloat.y) / 2);
                                }

                                rectangleCentre.setCentreX(centreX);
                                rectangleCentre.setCentreY(centreY);
                                rectangleCentreList.add(rectangleCentre);
                                // 匹配完后 清除
                                sb.delete(0, sb.length());
                            }
                        }


                        public void renderImage(ImageRenderInfo arg0) {
                            // nothing
                        }

                        public void endTextBlock() {
                            // nothing
                        }

                        public void beginTextBlock() {
                            // nothing
                        }
                    });
                }
                if (rectangleCentreList.isEmpty()) {
                    return null;
                }

                RectangleCentre rectangleCentre;
                for (int i = 0; i < rectangleCentreList.size(); i++) {
                    rectangleCentre = rectangleCentreList.get(i);
                    if (i > 0) {
                        // 多次签名，得重新读取
                        reader = new PdfReader(signPDFData.toByteArray());
                    }
                    // 创建临时字节流 重复使用
                    signPDFData = new ByteArrayOutputStream();

                    File temp = new File(signPdfSrcFile.getParent(), System.currentTimeMillis() + ".pdf");
                    // 创建签章工具
                    stp = PdfStamper.createSignature(reader, signPDFData, '\0', temp, true);
                    stp.setFullCompression();
                    PdfSignatureAppearance sap = stp.getSignatureAppearance();
                    sap.setReason("数字签名，不可改变");
                    // 获取数字签章属性对象，设定数字签章的属性
                    sap = stp.getSignatureAppearance();
                    // 设置签章图片
                    sap.setSignatureGraphic(image);
                    // 设置签章级别
                    sap.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                    // 设置签章的显示方式，如下选择的是只显示图章（还有其他的模式，可以图章和签名描述一同显示）
                    sap.setRenderingMode(RenderingMode.GRAPHIC);
                    // 设置签章位置 图章左下角x，原点为pdf页面左下角，图章左下角y，图章右上角x，图章右上角y
                    sap.setVisibleSignature(rectangleCentre.getRectangle(image), rectangleCentre.getPage(), null);

                    // 签章算法 可以自己实现
                    // 摘要算法
                    ExternalDigest digest = new BouncyCastleDigest();
                    // 签章算法
                    ExternalSignature signature = new PrivateKeySignature(key, DigestAlgorithms.SHA1, null);
                    // 进行盖章操作 CMS高级电子签名(CAdES)的长效签名规范
                    MakeSignature.signDetached(sap, digest, signature, chain, null, null, null, 0, CryptoStandard.CMS);
                    temp.delete();//删除pdf临时文件
                }
            } else {
                // 临时pdf文件
                File temp = new File(signPdfSrcFile.getParent(), System.currentTimeMillis() + ".pdf");
                stp = PdfStamper.createSignature(reader, signPDFData, '\0', temp, true);
                stp.setFullCompression();
                PdfSignatureAppearance sap = stp.getSignatureAppearance();
                sap.setReason("数字签名，不可改变");
                sap.setImageScale(0);
                sap.setSignatureGraphic(image);
                sap.setRenderingMode(RenderingMode.GRAPHIC);
                // 是对应x轴和y轴坐标
                sap.setVisibleSignature(new Rectangle(x, y, x + 185, y + 68), 1,
                        UUID.randomUUID().toString().replaceAll("-", ""));
                stp.getWriter().setCompressionLevel(5);
                ExternalDigest digest = new BouncyCastleDigest();
                ExternalSignature signature = new PrivateKeySignature(key, DigestAlgorithms.SHA512, provider.getName());
                MakeSignature.signDetached(sap, digest, signature, chain, null, null, null, 0, CryptoStandard.CADES);
                temp.delete();//删除pdf临时文件
            }
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
        //byte[] fileData = sign("123456", "E:/keystore.p12", "E:/1643-filled.pdf", "E:/sign.png", "电子签章2");
        byte[] fileData = sign("123456", "E:/keystore.p12", "E:/signed.pdf", "E:/sign.png", "电子签章1");
        if (fileData != null) {
            FileOutputStream f = new FileOutputStream(new File("E:/signed_2.pdf"));
            f.write(fileData);
            f.close();
        }
    }
}
