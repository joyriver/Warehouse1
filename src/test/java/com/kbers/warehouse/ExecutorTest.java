package com.kbers.warehouse;

import com.amzass.service.common.ApplicationContext;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ExecutorTest {

    @Test
    void dbManage(){
        Executor nj = ApplicationContext.getBean(Executor.class);
        nj.readSpreadIdAccEmail("38EU|1_e5lFtIz7NsEDuU-9jOinubHxbxNV0ww2dGJt-e1pwM|spirifiofgodeu@gmail.com",false, false);
    }

}