package org.fui;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

public class Snippet {
    // 利用模板生成pdf
    public static void fillTemplate() {
        // 模板路径
        String templatePath = "E:/1643.pdf";
        // 生成的新文件路径
        String newPDFPath = "E:/1643-" + System.currentTimeMillis() + ".pdf";
        PdfReader reader;
        FileOutputStream out;
        ByteArrayOutputStream bos;
        PdfStamper stamper;
        try {
            out = new FileOutputStream(newPDFPath);// 输出流
            reader = new PdfReader(templatePath);// 读取pdf模板
            bos = new ByteArrayOutputStream();
            stamper = new PdfStamper(reader, bos);
            AcroFields form = stamper.getAcroFields();

            String[] str = {"NO-99882-01", "2018", "08", "17", "2018", "张三", "李四", "王五"};

            Set<String> set = form.getFields().keySet();
            int i = 0;
            for (String name : set) {
                System.out.println(name);
                form.setField(name, str[i++]);
            }
            stamper.setFormFlattening(true);// 如果为false那么生成的PDF文件还能编辑，一定要设为true
            stamper.close();


            Document doc = new Document();
            PdfCopy copy = new PdfSmartCopy(doc, out);
            doc.open();
            //pdf模版的页数
            int pageNumber = reader.getNumberOfPages();
            for (int p = 1; p < pageNumber + 1; p++) {
                PdfImportedPage importPage = copy.getImportedPage(new PdfReader(bos.toByteArray()), p);
                copy.addPage(importPage);
            }
            doc.close();
        } catch (IOException e) {
            System.out.println("读取pdf文件异常或文件不存在");
        } catch (DocumentException e) {
            System.out.println("解析pdf文件异常");
        }

    }

    public static void main(String[] args) {
        fillTemplate();
    }
}
