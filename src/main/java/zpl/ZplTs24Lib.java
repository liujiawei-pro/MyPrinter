package zpl;

import sun.awt.AppContext;

import javax.print.*;
import javax.print.attribute.standard.PrinterName;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 采用点阵字库ts24.lib, 需要字体库
 *
 */
public class ZplTs24Lib {
    // 打印机完整路径
    private String printerURI = null;
    // 打印机服务
    private PrintService printService = null;
    private byte[] dotFont;
    // Set Darkness设置色带颜色的深度 0-30
    private String darkness = "~SD30";
    // Print Width打印宽度0-1500
    private String width = "^PW500";
    // Label Length标签长度0-x(暂无作用)
    private String length = "^LL300";
    // ASCII Transparency和多字节亚洲编码
    private String coding = "^CI26";
    // 标签格式以^XA开始
    private String begin = "^XA";
    // 标签格式以^XZ结束
    private String end = "^XZ";
    // 打印内容
    private String content = "";
    // 打印的结果信息
    private String message = "";

    /**
     * 构造方法
     * @param printerURI 打印机路径
     */
    public ZplTs24Lib(String printerURI){
        this.printerURI = printerURI;
        //加载字体
        File file = new File("/ts24.lib");
        if(file.exists()){
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                dotFont = new byte[fis.available()];
                fis.read(dotFont);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("ts24.lib文件不存在");
        }
        //初始化打印机
        AppContext.getAppContext().put(PrintServiceLookup.class.getDeclaredClasses()[0], null);//刷新打印机列表
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null,null);
        if (services != null && services.length > 0) {
            for (PrintService service : services) {
                if (printerURI.equals(service.getName())) {
                    printService = service;
                    break;
                }
            }
        }
        if (printService == null) {
            System.out.println("没有找到打印机：["+printerURI+"]");
            //循环出所有的打印机
            if (services != null && services.length > 0) {
                System.out.println("可用的打印机列表：");
                for (PrintService service : services) {
                    System.out.println("["+service.getName()+"]");
                }
            }
        }else{
            System.out.println("找到打印机：["+printerURI+"]");
            System.out.println("打印机名称：["+printService.getAttribute(PrinterName.class).getValue()+"]");
        }
    }
    /**
     * 设置条形码
     * @param barcode 条码字符
     * @param zpl 条码样式模板
     */
    public void setBarcode(String barcode, String zpl) {
        content += zpl.replace("${data}", barcode);
    }

    /**
     * 中文字符、英文字符(包含数字)混合
     * @param str 中文、英文
     * @param x x坐标
     * @param y y坐标
     * @param eh 英文字体高度height
     * @param ew 英文字体宽度width
     * @param es 英文字体间距spacing
     * @param mx 中文x轴字体图形放大倍率。范围1-10，默认1
     * @param my 中文y轴字体图形放大倍率。范围1-10，默认1
     * @param ms 中文字体间距。24是个比较合适的值。
     */
    public void setText(String str, int x, int y, int eh, int ew, int es, int mx, int my, int ms) {
        byte[] ch = str2bytes(str);
        for (int off = 0; off < ch.length;) {
            //中文字符
            if (((int) ch[off] & 0x00ff) >= 0xA0) {
                try {
                    int qcode = ch[off] & 0xff;
                    int wcode = ch[off + 1] & 0xff;
                    content += String.format("^FO%d,%d^XG0000%01X%01X,%d,%d^FS\n", x, y, qcode, wcode, mx, my);
                    begin += String.format("~DG0000%02X%02X,00072,003,\n", qcode, wcode);
                    qcode = (qcode + 128 - 32) & 0x00ff;
                    wcode = (wcode + 128 - 32) & 0x00ff;
                    int offset = ((int) qcode - 16) * 94 * 72 + ((int) wcode - 1) * 72;
                    for (int j = 0; j < 72; j += 3) {
                        qcode = (int) dotFont[j + offset] & 0x00ff;
                        wcode = (int) dotFont[j + offset + 1] & 0x00ff;
                        int qcode1 = (int) dotFont[j + offset + 2] & 0x00ff;
                        begin += String.format("%02X%02X%02X\n", qcode, wcode, qcode1);
                    }
                    x = x + ms * mx;
                    off = off + 2;
                } catch (Exception e) {
                    e.printStackTrace();
                    //替换成X号
                    setChar(" ", x, y, eh, ew);
                    //注意间距更改为英文字符间距
                    x = x + es;
                    off = off + 2;
                }
            } else if (((int) ch[off] & 0x00FF) < 0xA0) {
                //英文字符
                setChar(String.format("%c", ch[off]), x, y, eh, ew);
                x = x + es;
                off++;
            }
        }
    }

    /**
     * 英文字符串(包含数字)
     * @param str 英文字符串
     * @param x x坐标
     * @param y y坐标
     * @param h 高度
     * @param w 宽度
     */
    public void setChar(String str, int x, int y, int h, int w) {
        content += "^FO" + x + "," + y + "^A0," + h + "," + w + "^FD" + str + "^FS";
    }
    /**
     * 英文字符(包含数字)顺时针旋转90度
     * @param str 英文字符串
     * @param x x坐标
     * @param y y坐标
     * @param h 高度
     * @param w 宽度
     */
    public void setCharR(String str, int x, int y, int h, int w) {
        content += "^FO" + x + "," + y + "^A0R," + h + "," + w + "^FD" + str + "^FS";
    }
    /**
     * 获取完整的ZPL
     * @return
     */
    public String getZpl() {
        return begin + content + end;
    }
    /**
     * 重置ZPL指令，当需要打印多张纸的时候需要调用。
     */
    public void resetZpl() {
        begin = "^XA" + darkness + width;
        end = "^XZ";
        content = "";
    }
    /**
     * 打印
     * @param zpl 完整的ZPL
     */
    public boolean print(String zpl){
        if(printService==null){
            message = "打印出错：没有找到打印机["+printerURI+"]";
            System.out.println("打印出错：没有找到打印机["+printerURI+"]");
            return false;
        }
        DocPrintJob job = printService.createPrintJob();
        byte[] by = zpl.getBytes();
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(by, flavor, null);
        try {
            job.print(doc, null);
            message = "已打印";
            System.out.println("已打印");
            return true;
        } catch (PrintException e) {
            message = "打印出错:"+e.getMessage();
            System.out.println("打印出错:"+e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public String getMessage(){
        return message;
    }
    /**
     * 字符串转byte[]
     * @param s
     * @return
     */
    private byte[] str2bytes(String s) {
        if (null == s || "".equals(s)) {
            return null;
        }
        byte[] abytes = null;
        try {
            abytes = s.getBytes("gb2312");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return abytes;
    }


    /**
     * 二維碼打印功能
     * @param p
     * @param bar0
     * @param x
     * @param y
     */
    public void zpl_QCode(ZplTs24Lib p, String bar0, int x, int y) {
        StringBuffer sb = new StringBuffer();
        sb.append("^FO" + x + "," + y);
        sb.append("^BQN,2,6");
        sb.append("^FDMM,");
        sb.append("A${data}");
        sb.append("^FS");
        p.setBarcode(bar0, sb.toString());
    }


    public static void main(String[] args) {

        // ZplTs24Lib p = new ZplTs24Lib("\\\\192.168.0.122\\ZDesigner GK888t (EPL)");
        ZplTs24Lib p = new ZplTs24Lib("ZDesigner GK888t (EPL)");

        p.setText("名称:20寸及以上触摸式无线生命体征监护屏", 15, 30, 25, 25, 13, 1, 1, 24);
        p.setText("型号:JC－CK34001", 15, 70, 25, 25, 13, 1, 1, 24);
        p.setText("品牌:戴尔", 15, 110, 25, 25, 13, 1, 1, 24);
        p.setText("存放地点:22L心肺听诊训练室", 15, 150, 25, 25, 13, 1, 1, 24);
        p.setText("购买日期:2019年8月6日", 15, 190, 25, 25, 13, 1, 1, 24);

        String bar1 = "1f4bb310884b4bf582c8f8ab42953eeb";

        p.zpl_QCode(p, bar1, 370, 55);

        String zpl = p.getZpl();
        System.out.println(zpl);
        boolean result = p.print(zpl);

        System.out.println(result);

    }
}