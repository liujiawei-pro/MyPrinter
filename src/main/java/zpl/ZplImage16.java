package zpl;

import org.apache.commons.lang3.StringUtils;
import sun.awt.AppContext;

import javax.print.*;
import javax.print.attribute.standard.PrinterName;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author ljw
 * 描述：生成位图，并转16进制字符
 */
public class ZplImage16 {

    String imgCodes = "", begin = "^XA", content = "", end = "^XZ";
    Integer textNo = 0;

    /**
     * 打印机地址
     */
    String printerURI;
    /**
     * 打印机
     */
    public static PrintService printService = null;

    public ZplImage16() {

    }

    public ZplImage16(String printerURI) {
        this.printerURI = printerURI;

        // 初始化打印机
        // 刷新打印机列表
        AppContext.getAppContext().put(PrintServiceLookup.class.getDeclaredClasses()[0], null);
        // 打印机列表
        PrintService[] services = getAllPrintList();
        if (services != null && services.length > 0) {
            for (PrintService service : services) {
                // 找到打印机对象
                if (printerURI.equals(service.getName())) {
                    printService = service;
                    break;
                }
            }
        }
        if (printService == null) {
            System.out.println("没有找到打印机：[" + printerURI + "]");
            //循环出所有的打印机
            if (services != null && services.length > 0) {
                System.out.println("可用的打印机列表：");
                for (PrintService service : services) {
                    System.out.println("[" + service.getName() + "]");
                }
            }
        } else {
            System.out.println("找到打印机：[" + printerURI + "]");
            System.out.println("打印机名称：[" + printService.getAttribute(PrinterName.class).getValue() + "]");
        }

    }

    /**
     * 获取当前电脑所有已添加的打印机列表
     * @return
     */
    public static PrintService[] getAllPrintList(){
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        return services;
    }

    /**
     * 重置命令
     */
    public void resetCommand() {
        imgCodes = "";
        begin = "^XA";
        content = "";
        end = "^XZ";
    }

    /**
     * 获取命令
     */
    public String getCommand() {
        return imgCodes +
                begin + "\n" +
                content +
                end;
    }

    /**
     * 中文生成编码
     *
     * @param text
     * @param size
     * @param x
     * @param y
     */
    public void zplCN(String text, int size, int x, int y) {
        BufferedImage img = createImage(text, size);
        String codeData = convertImageToCode(img);
        String t = ((img.getWidth() / 8 + ((img.getWidth() % 8 == 0) ? 0 : 1)) * img.getHeight()) + "";
        String w = (img.getWidth() / 8 + ((img.getWidth() % 8 == 0) ? 0 : 1)) + "";
//		String zpl = "~DGOUTSTR01," + t + "," + w + "," + codeData;
        ++textNo;
        imgCodes += "~DGtext" + textNo + "," + t + "," + w + "," + codeData + "\n";

        content += "^FO" + x + "," + y + "\n" +
                "^XGtext" + textNo + ",1,1^FS\n";
    }

    /**
     * 生成图片
     *
     * @param text
     * @param size
     * @return
     */
    public final BufferedImage createImage(String text, int size) {
        size = size == 0 ? 28 : size;
        Font font = new Font("宋体", Font.BOLD, size);
        JTextField txt = new JTextField();
        txt.setText(text);
        txt.setFont(font);

        int width = txt.getPreferredSize().width;
        int height = txt.getPreferredSize().height;

        BufferedImage bi = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = (Graphics2D) bi.getGraphics();
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, width, height);

        g2.setFont(font);
        g2.setPaint(Color.BLACK);

        FontRenderContext context = g2.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(text, context);
        double x = (width - bounds.getWidth()) / 2;
        double y = (height - bounds.getHeight()) / 2;
        double ascent = -bounds.getY();
        double baseY = y + ascent;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.drawString(text, (int) x, (int) baseY);

        return bi;
    }

    /**
     * 图片对象转16进制编码
     *
     * @param img
     * @return
     */
    public final String convertImageToCode(BufferedImage img) {
        StringBuffer sb = new StringBuffer();
        long clr = 0, n = 0;
        int b = 0;
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                b = b * 2;
                clr = img.getRGB(j, i);
                String s = String.format("%X", clr);

                if (s.substring(s.length() - 6, s.length() - 6 + 6).compareTo(
                        "BBBBBB") < 0) {
                    b++;
                }
                n++;
                if (j == (img.getWidth() - 1)) {
                    if (n < 8) {
                        b = b * (2 ^ (8 - (int) n));
                        sb.append(StringUtils.leftPad(String.format("%X", b),
                                2, "0"));
                        // sb.append(String.format("%X", b).PadLeft(2, '0'));
                        b = 0;
                        n = 0;
                    }
                }
                if (n >= 8) {
                    sb.append(StringUtils.leftPad(String.format("%X", b), 2,
                            "0"));
                    // sb.append(String.format("%X", b).PadLeft(2, '0'));
                    b = 0;
                    n = 0;
                }
            }
            sb.append("\n");
        }
        return sb.toString();

    }

    /**
     * 触发打印
     *
     * @param str
     * @throws PrintException
     */
    public void runPrint(String str) throws PrintException {
        /*// 获取默认打印机
        printService = PrintServiceLookup.lookupDefaultPrintService();*/
        if (printService == null) {
            System.out.println("没有发现条码打印机.");
            return;
        }
        DocPrintJob job = printService.createPrintJob();
        byte[] by = str.getBytes();
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(by, flavor, null);
        job.print(doc, null);
    }

    /**
     * 二维码
     *
     * @param text
     * @param x
     * @param y
     */
    public void zplQCode(String text, int x, int y) {
        StringBuffer sb = new StringBuffer();
        sb.append("^FO" + x + "," + y);
        sb.append("^BQN,2,6");
        sb.append("^FDMM,");
        sb.append("A");
        sb.append(text);
        sb.append("^FS");
        content += sb.toString();
    }

    public static void main(String[] args) throws PrintException {
        // ZplImage16 obj = new ZplImage16("\\\\192.168.0.122\\ZDesigner GK888t (EPL)");
        ZplImage16 obj = new ZplImage16("ZDesigner GK888t (EPL)");

        obj.resetCommand();

        obj.zplCN("名称：20寸及以上触摸式", 25, 15, 30);
        obj.zplCN("型号：JC－CK34001", 25, 15, 70);
        obj.zplCN("品牌：戴尔", 25, 15, 110);
        obj.zplCN("存放地点：22L心肺听诊训练室", 25, 15, 150);
        obj.zplCN("购买日期：2019年8月6日", 25, 15, 190);

        obj.zplQCode("1f4bb310884b4bf582c8f8ab42953eeb", 380, 55);

        String command = obj.getCommand();
        obj.runPrint(command);
        System.out.println(command);

    }
}