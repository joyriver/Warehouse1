package com.kbers.warehouse;

import com.amzass.service.common.ApplicationContext;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;

public class CopyJob implements Job {

    @Inject
    DBManager dbManager;

    public static void main(String[] args) throws JobExecutionException {
        new CopyJob().execute(null);
    }

    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        Logger logger = Logger.getLogger(CopyJob.class);
        SimpleLayout layout = new SimpleLayout();
        FileAppender appender = null;

        // Set the logger level to Level.INFO
        logger.setLevel(Level.INFO);
        // This request will be disabled since Level.DEBUG < Level.INFO.
        logger.debug("This is debug.");
        // These requests will be enabled.
//        try {
//            appender = new FileAppender(layout, "Nj Monitor.log", false);
//        } catch (Exception e) {
//            logger.error("生成log失败：", e);
//        }
//        logger.addAppender(appender);
        try {
            exe();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static void exe() throws IOException {
        NjComparison njComparison = ApplicationContext.getBean(NjComparison.class);
//        njComparison.clearNJ();

        if(Constants.getWeekDay() == 1 || Constants.getWeekDay() == 4){
            njComparison.clearNJ();
        }


        Executor EXE = ApplicationContext.getBean(Executor.class);
        EXE.execute(false, false);

        njComparison.scan();


//        JpComparison jpCompare = ApplicationContext.getBean(JpComparison.class);
//        jpCompare.clearJP();
//
//        try {
//            EXE.execute(true, false);
//        } catch (ServiceException e) {
//            e.printStackTrace();
//        }
//
//        jpCompare.scan();


    }


}  