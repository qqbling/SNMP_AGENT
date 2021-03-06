package com.gxf.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Util {
	/**
     * 将byte数组转化为Long类型
     * 
     * @param array
     * @return
     */
    public long bytesToLong(byte[] array)
    {
        return ((((long) array[0] & 0xff) << 56) | (((long) array[1] & 0xff) << 48) | (((long) array[2] & 0xff) << 40)
                | (((long) array[3] & 0xff) << 32) | (((long) array[4] & 0xff) << 24)
                | (((long) array[5] & 0xff) << 16) | (((long) array[6] & 0xff) << 8) | (((long) array[7] & 0xff) << 0));
    }
    
    /**
     * 获取当前工程目录
     * @return
     */
    public String getCurrentProjectPath(){
    	String curPath = System.getProperty("user.dir");
    	
    	return curPath;
    }
    
    /**
     * 解压播放方案
     * 这里用apache ant进行解压，java自带的速度慢，且不支持压缩包中有中文
     * @param solutionName
     */
    public void unzipSolution(String displayName, String solutionName){
    	int BUFFER = 2048;  	
        
        String filePath = getCurrentProjectPath() + File.separator + "playSolutions" + File.separator + displayName +
        		File.separator + displayName + "+" + solutionName + ".zip";
        String desPath = filePath.substring(0, filePath.lastIndexOf("\\")) + "\\";
        
        //删除播放方案图片，避免解压合并文件，造成多余图片
        File desFileTem = new File(desPath);
        //存在先删除在解压
        if(desFileTem.exists())
        	desFileTem.delete();
        
        int count = 0;
        byte[] a = new byte[BUFFER];
        File file = new File(filePath);
        if (!file.exists())
        {
            return;
        }
        try
        {
            ZipFile zipfile = new ZipFile(file);
            Enumeration<? extends ZipEntry> enu = zipfile.getEntries();
            while (enu.hasMoreElements())
            {
                ZipEntry entry = enu.nextElement();
                // 如果zip条目为目录，则直接创建
                if (entry.isDirectory())
                {
                    new File(desPath + entry.getName()).mkdirs();
                    continue;
                }
                File desFile = new File(desPath + entry.getName());
                if (!desFile.exists())
                {
                	if(!desFile.getParentFile().exists())
                		desFile.getParentFile().mkdir();
                    desFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(desFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER);
                BufferedInputStream zis = new BufferedInputStream(
                        zipfile.getInputStream(entry));
                while ((count = zis.read(a, 0, BUFFER)) != -1)
                {
                    bos.write(a, 0, count);
                }
                bos.flush();
                bos.close();
                zis.close();
            }
            zipfile.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * 解析configxml
     * @return
     */
//    public Config parseConfigXml(String solutionName){
//    	//解析结果
//    	Config config = new Config();
//    	//要解析的xml文件
//    	String curProjectPath = getCurrentProjectPath();
//    	String configXmlPath = curProjectPath + File.separator + "playSolutions" + File.separator 
//    			+ solutionName + File.separator + solutionName + ".xml"; 
//    	File configXmlFile = new File(configXmlPath);
//    	//使用SAXReader解析
//    	SAXReader reader = new SAXReader();
//    	try {
//			Document document = reader.read(configXmlFile);
//			Element root = document.getRootElement();
//			//style
//			Element styleElement = root.element("style");
//			config.setPlayStyle(Integer.parseInt(styleElement.getText()));
//			//timeInterval
//			Element timeIntervalElement = root.element("timeInterval");
//			config.setPlayTimeInterval(Integer.parseInt(timeIntervalElement.getText()));
//			//date_start
//			Element date_startElement = root.element("date_start");
//			Element day_startElement = date_startElement.element("day_start");
//			config.setDay_start(Integer.parseInt(day_startElement.getText()));
//			Element month_startElement = date_startElement.element("month_start");
//			config.setMonth_start(Integer.parseInt(month_startElement.getText()));
//			Element year_startElement = date_startElement.element("year_start");
//			config.setYear_start(Integer.parseInt(year_startElement.getText()));
//			//date_end
//			Element date_endElement = root.element("date_end");
//			Element day_endElement = date_endElement.element("day_end");
//			config.setDay_end(Integer.parseInt(day_endElement.getText()));
//			Element month_endElement = date_endElement.element("month_end");
//			config.setMonth_end(Integer.parseInt(month_endElement.getText()));
//			Element year_endElement = date_endElement.element("year_end");
//			config.setYear_end(Integer.parseInt(year_endElement.getText()));
//			//time_start
//			Element time_startElement = root.element("time_start");
//			Element second_startElement = time_startElement.element("second_start");
//			config.setSec_start(Integer.parseInt(second_startElement.getText()));
//			Element minute_startElement = time_startElement.element("minute_start");
//			config.setMin_start(Integer.parseInt(minute_startElement.getText()));
//			Element hour_startElement = time_startElement.element("hour_start");
//			config.setHour_start(Integer.parseInt(hour_startElement.getText()));
//			//time_end
//			Element time_endElement = root.element("time_end");
//			Element second_endElement = time_endElement.element("second_end");
//			config.setSec_end(Integer.parseInt(second_endElement.getText()));
//			Element minute_endElement = time_endElement.element("minute_end");
//			config.setMin_end(Integer.parseInt(minute_endElement.getText()));
//			Element hour_endElement = time_endElement.element("hour_end");
//			config.setHour_end(Integer.parseInt(hour_endElement.getText()));
//			//weekdays
//			Element weekdaysElement = root.element("weekdays");
//			boolean temp_weeks[] = config.getWeekdays();
//			int temp_index = 0;
//			for(Iterator<Element> it = weekdaysElement.elementIterator(); it.hasNext();){
//				Element element = (Element) it.next();
//				
//				temp_weeks[temp_index ++] = (element.getText().equals("0") ? false : true);
//				
//			}
//			
//			
//		} catch (DocumentException e) {
//
//			e.printStackTrace();
//		}
//    	
//    	return config;
//    }    
    
    /**
     * 通过显示屏名、播放方案名找到xml，解析
     * @param displayName
     * @param playSolutionName
     * @return
     */
    public Map<String, PlayControl> parseXml(String displayName, String playSolutionName){
//    	List<PlayControl> listOfPlayControl = new ArrayList<PlayControl>();
    	
    	Map<String, PlayControl> playControls = new HashMap<String ,PlayControl>();
    	
    	//获取xml路径
    	String curProjectPath = getCurrentProjectPath();
    	String configXmlPath = curProjectPath + File.separator + "playSolutions" + File.separator 
    			+ displayName + File.separator + playSolutionName + File.separator + playSolutionName + ".xml"; 
    	File configXmlFile = new File(configXmlPath);
    	
    	//使用SAXReader解析
    	SAXReader reader = new SAXReader();
    	try{
    		Document document = reader.read(configXmlFile);
    		//根节点
			Element root = document.getRootElement();
			for(Iterator<Element> it_picture = root.elementIterator(); it_picture.hasNext();){
				Element picture = (Element) it_picture.next();
				//获取图片名称在xml中
				String picName = picture.attribute("name").getValue();
				
				//解析获得的控制信息
				PlayControl playControl = new PlayControl();
				playControl.setPicName(picName);
				for(Iterator<Element> it_in = picture.elementIterator(); it_in.hasNext();){
					//type
					Element type = (Element) it_in.next();
					playControl.setPlayType(Integer.parseInt(type.getText()));
					//timeinterval
					Element timeInterval = (Element) it_in.next();
					playControl.setTimeInterval(Integer.parseInt(timeInterval.getText()));
					//date_start 
					Element date_start = (Element) it_in.next();
					playControl.setDateTimeStart(Date.valueOf(date_start.getText()));
					//date_end
					Element date_end = (Element) it_in.next();
					playControl.setDateTimeEnd(Date.valueOf(date_end.getText()));
					//time_start
					Element time_start = (Element) it_in.next();
					playControl.setTimeStart(Time.valueOf(time_start.getText()));
					//time_end
					Element time_end = (Element) it_in.next();
					playControl.setTimeEnd(Time.valueOf(time_end.getText()));
					//weekdays
					Element weekdays = (Element) it_in.next();
					playControl.setWeekdays(weekdays.getText());
					//playOrder
					Element playOrder = (Element) it_in.next();
					playControl.setPlayOrder(Integer.valueOf(playOrder.getText()));
				}//for
				playControls.put(picName, playControl);
			}//for
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	
    	return playControls;
    }

}

