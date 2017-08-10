package com.kbers.warehouse;

import com.amzass.service.common.ApplicationContext;
import com.amzass.service.common.TaskScheduler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TriggerMan extends JFrame {

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    TriggerMan frame = new TriggerMan();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    private TriggerMan() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 356, 231);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        try {
            this.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/NewJesery.png")));
            this.setTitle("New Jesery monitor定时触发器");
            this.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JButton btnNewButton = new JButton("Trigger me");
        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {

                SimpleDateFormat DateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                Date d = new Date();
                String returnstr = DateFormat.format(d);


                try {
                    System.out.println(returnstr + "System time");
                    ApplicationContext.getBean(TaskScheduler.class).startJob("0 1 1 1/1 * ? *", CopyJob.class);
                    System.out.println("Start New Jesery job!");


                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });
        btnNewButton.setBounds(123, 79, 108, 23);
        contentPane.add(btnNewButton);
    }
}
