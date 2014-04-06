/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alljoyn.bus.sample.chat;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.alljoyn.bus.BusException;

/**
 *
 * @author admin
 */
public class App {

    static int ser_cli = 0;
    static int service_or_client=0; //1 is for server 2 for client 
    static GUI_SmartJoyn gui;
    public static void type(int typ) {
        ser_cli = typ;
    }

    public static void set_channel_nickname(String channel_name,String nickname){
        gui.Set_channel_nick_name(channel_name, nickname);
    }
    
    public static void on_close(){
        service_or_client=0;
        gui.original_state();
    }
    public static void main(String[] args) throws BusException, InterruptedException {
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               gui= new GUI_SmartJoyn();
               gui.setVisible(true);
            }
        });
        Thread t;
        while (true) {
            while (ser_cli == 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("There is an exception in App");
                }
            }
            if (ser_cli == 1) {
                t = new serviceThread();
                t.start();
                ser_cli = 0;
                service_or_client=1;
            }
            if (ser_cli == 2) {
                t= new clientThread();
                t.start();
                ser_cli = 0;
                service_or_client=2;
            }
            if (ser_cli == 3) {
                if(service_or_client==1){
                    Service.set_running(false);
                }
                if(service_or_client==2){
                    Client.set_running(false);
                }
                service_or_client=0;
                ser_cli=0;
            }
            if(ser_cli == 4){
                if(service_or_client==1){
                    Service.set_running(false);
                }
                if(service_or_client==2){
                    Client.set_running(false);
                }
            }
        }
    }
}

class clientThread extends Thread {

    public clientThread() {

    }

    public void run() {
        try {
            Client.run_client(true);
        } catch (BusException ex) {
            Logger.getLogger(clientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(clientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

class serviceThread extends Thread {

    public serviceThread() {

    }

    public void run() {
        try {
            Service.run_service(true);
        } catch (BusException ex) {
            Logger.getLogger(serviceThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
