package com.tb.gateway;

import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class Test {
    public static void main(String[] args) {
        String fileName = "/Users/wushuo/Pictures/IMG_6163.jpg";
        byte[] bytes = FileUtil.readBytes(fileName);
        Image img = Img.from(ImgUtil.toImage(bytes))
                .setPositionBaseCentre(false)
                .scale(364, 407, null)
                .getImg();
        bytes = ImgUtil.toBytes(img, "jpg");
        while (bytes.length / 1024 > 200) {
            System.out.println(bytes.length);
            bytes = zip(bytes);
        }
        FileUtil.writeBytes(bytes, "/Users/wushuo/Pictures/a.jpeg");
    }

    public static byte[] zip(byte[] bytes) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Img img = Img.from(new ByteArrayInputStream(bytes));
        img.setQuality(0.2).write(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

}
