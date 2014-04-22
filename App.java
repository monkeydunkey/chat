/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alljoyn.bus.sample.chat;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.alljoyn.bus.BusException;

/**
 *
 * @author K!LL3R
 */
/**
 * The is the starting point of our application. It creates the main GUI. It
 * gives the user the option to create or join a channel using the channel
 * setting option. The Call notify allows the user to see missed calls, ask for
 * key from other devices and enable call notification. The Group chat option
 * allows the user to chat with all the devices connected to the channel. The
 * Group play option allows the user if server, the option to chose music files
 * and control the music player of other devices, if the user is a client he/she
 * could just have option whether or not to play the music on their device
 */
public class App {

    static int ser_cli = 0;         //This stores the type of operation requested by the user 1-server, 2-client, 3-stop client/service, 4-Ask for key, 5-call notifications 6-group chat 7-Group Play
    static int service_or_client = 0; //1 is for server 2 for client 
    static MainGUI gui;
    static String message;          //This the message for the group chat feature

    public static void type(int typ) {
        ser_cli = typ;
    }

    public static void set_message(String s) {
        message = s;
    }

    public static void set_channel_nickname(String channel_name, String nickname) {
        gui.Set_channel_nick_name(channel_name, nickname);
    }

    public static void on_close() {
        service_or_client = 0;
        gui.original_state();
    }

    public static void main(String[] args) throws BusException, InterruptedException {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                gui = new MainGUI();
                gui.setVisible(true);
            }
        });
        Thread t;
        System.out.println(service_or_client);
        while (true) {
            while (ser_cli == 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("There is an exception in App");
                }
            }
            //if it is called it initiates the server on a new Thread
            if (ser_cli == 1) {
                t = new serviceThread();
                t.start();
                ser_cli = 0;
                service_or_client = 1;
            }
            //if called it initiates the client on a new Thread
            if (ser_cli == 2) {
                t = new clientThread();
                t.start();
                ser_cli = 0;
                service_or_client = 2;
            }
            //it is called to disconnect the user from the connected/created channel
            if (ser_cli == 3) {
                if (service_or_client == 1) {
                    Service.set_running(false);
                }
                if (service_or_client == 2) {
                    Client.set_running(false);
                }
                service_or_client = 0;
                ser_cli = 0;
            }
            //it is called when the user initiates the ask key mechanism 
            if (ser_cli == 4) {
                if (service_or_client == 1) {
                    Service.ask_key();
                }
                if (service_or_client == 2) {
                    Client.ask_key();
                }
                ser_cli = 0;
            }
            //it is called when the user wants to view the missed calls
            if (ser_cli == 5) {
                if (service_or_client == 1) {
                    Service.see_missed_calls();
                }
                if (service_or_client == 2) {
                    Client.see_missed_calls();
                }
                ser_cli = 0;
            }
            //It is called if the user wants to send a meesage to all the users on the channel
            if (ser_cli == 6) {
                if (service_or_client == 1) {
                    Service.send_msg(message);
                }
                if (service_or_client == 2) {
                    Client.send_msg(message);
                }
                ser_cli = 0;
            }

            if (ser_cli == 7) {
                if (service_or_client == 1) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            new GroupPlayGUI().setVisible(true);
                        }
                    });
                }
                if (service_or_client == 2) {
                    
                }
                ser_cli = 0;
            }
        }
    }
}
// This runs the client part of the code in a new thread

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
        } catch (FileNotFoundException ex) {
            Logger.getLogger(clientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(clientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

// This runs the client part of the code in a new thread
class serviceThread extends Thread {

    public serviceThread() {

    }

    public void run() {
        try {
            Service.run_service(true);
        } catch (BusException ex) {
            Logger.getLogger(serviceThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(serviceThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(serviceThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
