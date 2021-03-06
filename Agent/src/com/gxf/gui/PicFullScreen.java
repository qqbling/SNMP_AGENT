package com.gxf.gui;

import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.gxf.util.Config;
import com.gxf.util.CoorPoint;
import com.gxf.util.PicFilter;
import com.gxf.util.PlayControl;
import com.gxf.util.Util;

public class PicFullScreen {
	private Display display;
	private Shell curShell;
	
	// 文件存储
	private File currentPic = null;
	private File[] pics;
	private int picPoint = 0;
	
	
	
	//显示图片
	private Canvas canvas_picshow;		
	
	//当前需要显示的image
	private Image curImage;
	
	//停止播放
	private boolean isStop = false;

	
	//播放方案配置
	private Config config;
	private Map<String, PlayControl> mapOfPlayControl;  			//图片名称和控制信息map
	
	//播放方案名称
	public static String solutionName;
	public static String displayName;
	
	//工具类
	private Util util = new Util();
	//是否重新导入播放方案
	public static boolean isReloadSolution = false;
	
	//存放所有播放方案的文件夹
	private final String DIC_NAME_PLAY_SOLUTIONS = "playSolutions";
	
	//--------------------------------播放特效控制-------------------------
	private CoorPoint points[] = new CoorPoint[4];
	private int heightOfCanvas;
	private int widthOfCanvas;
	private int pointsIndex;
	private final int DISTANCE = 70;
	private final int SPEED = 100;
	private Thread playStyleThread[] = new Thread[4];
	
	public PicFullScreen(String displayName, String solutionName){
		PicFullScreen.solutionName = solutionName;
		PicFullScreen.displayName = displayName;
	}
	public void open(){
		//初始化控件
		init();		
		curShell.open();
		
		while(!curShell.isDisposed()){
			if(!display.readAndDispatch()){
				display.sleep();
			}
		}
	}
	
	//获取和设置播放方案名
	public String getSolutionName() {
		return solutionName;
	}
	public void setSolutionName(String solutionName) {
		PicFullScreen.solutionName = solutionName;
	}
	/**
	 * 对控件进行初始化
	 */
	public void init(){		
		display = Display.getDefault();
		curShell = new Shell(display, SWT.NO_TRIM);	
		curShell.setText("全屏显示");

		//获取屏幕宽度和高度
		int iSreenWith = Toolkit.getDefaultToolkit().getScreenSize().width;
		int iSreenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;		
		
		//设置shell宽度和高度
		curShell.setSize(iSreenWith, iSreenHeight);
		
		//添加键盘和鼠标监听器
		curShell.addKeyListener(new KeyListenerImpl());		

		curShell.setLocation(0, 0);
		
		//创建composite和canvas显示图片
		Composite composite_pic = new Composite(curShell, SWT.NONE);
		composite_pic.setBounds(curShell.getBounds().x, curShell.getBounds().y, curShell.getBounds().width, curShell.getBounds().height);
		canvas_picshow = new Canvas(composite_pic, SWT.NONE);
		canvas_picshow.setBounds(curShell.getBounds().x, curShell.getBounds().y, curShell.getBounds().width, curShell.getBounds().height);
		canvas_picshow.addMouseListener(new MouseListenerImp());
		canvas_picshow.addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(PaintEvent arg0) {
				if(curImage != null)
//					arg0.gc.drawImage(curImage, 0, 0);
					arg0.gc.drawImage(curImage, points[pointsIndex].getX(), points[pointsIndex].getY());
			}
		});		
		
		//导入播放方案
		importPlaySolution(displayName, solutionName);
		
		//设置curPic
		currentPic = pics[picPoint];	
				
		//启动自动播放线程
		PlayPicThread picPlayThread = new PlayPicThread();
		picPlayThread.start();
		
		//--------------------------图片播放特效设置----------------------------------
		//获取画布的大小，用于图片播放特效 
		widthOfCanvas = iSreenWith;
		heightOfCanvas = iSreenHeight;
		
		initPoints();
		initPlayStyleThread();
	}
		
	/**
	 * 按键监听器，这里主要是为了监听esc退出全屏显示
	 * @author Administrator
	 *
	 */
	class KeyListenerImpl implements KeyListener{

		@Override
		public void keyPressed(KeyEvent arg0) {
			if(arg0.keyCode == SWT.ESC)
			{
				//退出全屏，设置前面shell显示出来
				curShell.dispose();
				//停止全屏播放线程
				setIsStop(true);
				//设置前面shell可见
				PicPlayer.curShell.setVisible(true);
			}
			
		}

		@Override
		public void keyReleased(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/**
	 * 设置需要播放的图片
	 * @param pics
	 */
	public void setPic(File pics[]){
		this.pics = pics;
	}
	
	/**
	 * 从picPoint开始播放
	 * @param picPoint
	 */
	public void setPicPoint(int picPoint){
		this.picPoint = picPoint;
	}
		
	/**
	 * 将图片显示到画布上
	 */
	public void drawImage(){
		ImageData imageData = new ImageData(currentPic.getPath());
		int width = curShell.getBounds().width;
		int height = curShell.getBounds().height;
		imageData = imageData.scaledTo(width, height);
		curImage = new Image(curShell.getDisplay(), imageData);
		
		canvas_picshow.redraw();
		
	}
	
	/**
	 * 自动播放图片线程
	 * @author Administrator
	 *
	 */
	class PlayPicThread extends Thread{
		public void run(){
			while(!getIsStop()){
				if(getIsReloadSolution()){							//重新导入播放方案
					System.out.println("接收到新播放方案，重新加载播放方案!");
					importPlaySolution(displayName, solutionName);
					setIsReloadSolution(false);						//避免一直导入	
				}
				picPoint = picPoint % pics.length;
				currentPic = pics[picPoint];			
				
				try {
//					sleep((long)(config.getPlayTimeInterval() * 1000));
					//获取播放间隔
					int timeInterval = getTimeInterval(picPoint);
					if(timeInterval == -1)
					{
						picPoint++;
						continue;
					}
					Display.getDefault().syncExec(new Runnable() {
						
						@Override
						public void run() {
							drawImage();
							
						}
					});
					//-----------------图片显示特效---------------
					initPlayStyleThread();
					pointsIndex = (pointsIndex + 1) % 4;
					playStyleThread[pointsIndex].start();
					sleep((long)(timeInterval * 1000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				picPoint++;
			}//while				
		}
	}
	
	//同步访问isStop
	public synchronized void setIsStop(boolean isStop){
		this.isStop = isStop;
	}
	public synchronized boolean getIsStop(){
		return isStop;
	}
	
	/**
	 * 鼠标监听器
	 * @author Administrator
	 *
	 */
	class MouseListenerImp implements MouseListener{
		
		//双击鼠标退出全屏
		@Override
		public void mouseDoubleClick(MouseEvent arg0) {
			//停止全屏播放线程
			setIsStop(true);
			//退出全屏，设置前面shell显示出来
			curShell.dispose();
			//设置前面shell可见
			PicPlayer.curShell.setVisible(true);
		}

		@Override
		public void mouseDown(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseUp(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/**
	 * 导入播放方案
	 */
	public void importPlaySolution(String displayName, String solutionName){		
		String projectPath = util.getCurrentProjectPath();
//		String filePath = projectPath + File.separator + DIC_NAME_PLAY_SOLUTIONS 
//							+ File.separator + solutionName;
		String filePath = projectPath + File.separator + DIC_NAME_PLAY_SOLUTIONS + File.separator + displayName
				+ File.separator + solutionName;
		//先解压文件
//		util.unzipSolution(displayName, solutionName);
		//文件夹中所有图片,生成File对象
		//这里图片文件不能直接从硬盘中读取
		//从xml中解析顺序生成文件对象，实现对图片播放顺序的控制
		//获取播放方案配置
		mapOfPlayControl = util.parseXml(displayName, solutionName);
		
		List<PlayControl> listOfPlayControl = new ArrayList<PlayControl>();
		for(Iterator<String> it = mapOfPlayControl.keySet().iterator(); it.hasNext();){
			String temp_picName = it.next();
			listOfPlayControl.add(mapOfPlayControl.get(temp_picName));
		}//for
		
		//排序
		Collections.sort(listOfPlayControl);
		pics = new File[listOfPlayControl.size()];
		for(int i = 0; i < pics.length; i++){
			pics[i] = new File(filePath + File.separator + listOfPlayControl.get(i).getPicName());
		}//for

		
		currentPic = pics[0];
		picPoint = 0;
		//显示图片到画布上
//		drawImage(); 
		
	}
	//isReloadSolution get和set方法
	public static synchronized boolean getIsReloadSolution() {
		return isReloadSolution;
	}
	public synchronized  static void  setIsReloadSolution(boolean isReloadSolution) {
		PicFullScreen.isReloadSolution = isReloadSolution;
	}
	
	/**
	 * 获取暂停播放时间
	 * @param picIndex
	 * @return
	 * -1表示不能播放
	 * >0表示播放几秒
	 */
	private int getTimeInterval(int picIndex){
		String picName = pics[picIndex].getName();
		//默认可以播放
		PlayControl playControl = mapOfPlayControl.get(picName);
		
		//1.比较日期范围是否valid
		//2.比较时间范围是否valid
		//3.星期是否valid
		
		//比较日期+时间
		java.util.Date curDate = new java.util.Date();
		String beginDateStr = playControl.getDateTimeStart().toString() + " " + playControl.getTimeStart();
		String afterDateStr = playControl.getDateTimeEnd().toString() + " " + playControl.getTimeEnd();
		
		java.sql.Timestamp beginDate = java.sql.Timestamp.valueOf(beginDateStr);
		java.sql.Timestamp afterDate = java.sql.Timestamp.valueOf(afterDateStr);
		
		if(!(curDate.after(beginDate) && curDate.before(afterDate))){
			return -1;
		}
		
		//比较星期
		String weekdaysStr = playControl.getWeekdays();
		//获取当前星期
		Calendar c = Calendar.getInstance();
        int curWeekday = (c.get(Calendar.DAY_OF_WEEK) - 1);
        if(curWeekday < 0)
        	curWeekday = 6;
        if(weekdaysStr.charAt(curWeekday) == '0')
        	return -1;
        
//        System.out.println("curWeekdayStr = " + weekdaysStr + "curWeekday = " + curWeekday);
        
        //日期、时间和星期都valid获取播放间隔时间返回
        int timeInterval = playControl.getTimeInterval();
		
        return timeInterval;
	}
	
	
	
	//----------------------------------播放特效控制--------------------------------------------
		/**
		 * 初始化四种播放方式起始点
		 */
		private void initPoints(){
			//上左下右
			points[0] = new CoorPoint(0, heightOfCanvas);
			points[1] = new CoorPoint(widthOfCanvas, 0);
			points[2] = new CoorPoint(0, -heightOfCanvas);
			points[3] = new CoorPoint(-widthOfCanvas, 0);
		}
		
		/**
		 * 上拉特效
		 * @author Administrator
		 *
		 */
		class MoveUpThread extends Thread{
			@Override
			public void run(){
				initPoints();
				while(!Display.getDefault().isDisposed()){
					if(points[pointsIndex].getY() <= 0)
					{
						points[pointsIndex].setY(0);
						break;
					}//if
					
					points[pointsIndex].setY(points[pointsIndex].getY() - DISTANCE);
//					System.out.println("y = " + y);
					
					try {
						sleep(SPEED);
						if(!Display.getDefault().isDisposed())
							Display.getDefault().syncExec(new Runnable(){

							@Override
							public void run() {
								if(canvas_picshow != null)
									canvas_picshow.redraw();
								
							}
							
						});
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		
		/**
		 * 左拉
		 * @author Administrator
		 *
		 */
		class MoveLeftThread extends Thread{
			@Override
			public void run(){
				initPoints();
				
				while(!Display.getDefault().isDisposed()){
					
					if(points[pointsIndex].getX() <= 0)
					{
						points[pointsIndex].setX(0);
						break;
					}//if
					
					points[pointsIndex].setX(points[pointsIndex].getX() - DISTANCE);
					
					try {
						sleep(SPEED);
						if(!Display.getDefault().isDisposed())
							Display.getDefault().syncExec(new Runnable(){

							@Override
							public void run() {
								if(!canvas_picshow.isDisposed())
									canvas_picshow.redraw();								
							}
							
						});
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * 下拉
		 * @author Administrator
		 *
		 */
		class MoveDownThread extends Thread{
			@Override
			public void run(){
				initPoints();
				
				while(!Display.getDefault().isDisposed()){				
					if(points[pointsIndex].getY() >= 0)
					{
						points[pointsIndex].setY(0);
						break;
					}//if
					
					points[pointsIndex].setY(points[pointsIndex].getY() + DISTANCE);
					
					try {
						sleep(SPEED);
						if(!Display.getDefault().isDisposed())
							Display.getDefault().syncExec(new Runnable(){

							@Override
							public void run() {
								if(!canvas_picshow.isDisposed())
									canvas_picshow.redraw();								
							}
							
						});
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * 向右拉
		 * @author Administrator
		 *
		 */
		class MoveRightThread extends Thread{
			@Override
			public void run(){
				initPoints();
				
				while(!Display.getDefault().isDisposed()){
					
					if(points[pointsIndex].getX() >= 0)
					{
						points[pointsIndex].setX(0);
						break;
					}//if
					
					points[pointsIndex].setX(points[pointsIndex].getX() + DISTANCE);
					
					try {
						sleep(SPEED);
						if(!Display.getDefault().isDisposed())
							Display.getDefault().syncExec(new Runnable(){

							@Override
							public void run() {
								if(!canvas_picshow.isDisposed())
									canvas_picshow.redraw();
								
							}
							
						});
						
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * 切换播放方式
		 * @author Administrator
		 *
		 */
		class SwitchStyleThread extends Thread{		
			
			@Override
			public void run(){
				try {
					while(!getIsStop()){
						initPlayStyleThread();
						pointsIndex = (pointsIndex + 1) % 4;
						playStyleThread[pointsIndex].start();
						sleep(getTimeInterval(picPoint) * 1000);
					}
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			
		}
		
		/**
		 * 初始化播放特效线程
		 */
		private void initPlayStyleThread(){
			playStyleThread[0] = new MoveUpThread();
			playStyleThread[1] = new MoveLeftThread();
			playStyleThread[2] = new MoveDownThread();
			playStyleThread[3] = new MoveRightThread();

		}
}
